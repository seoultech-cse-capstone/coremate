package com.coremate.backend.avatar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coremate.backend.avatar.catalog.AvatarItem;
import com.coremate.backend.avatar.catalog.AvatarItemRepository;
import com.coremate.backend.avatar.catalog.type.AcquiredSource;
import com.coremate.backend.avatar.catalog.type.AvatarSlot;
import com.coremate.backend.avatar.equip.UserAvatarEquippedItem;
import com.coremate.backend.avatar.equip.UserAvatarEquippedItemRepository;
import com.coremate.backend.avatar.inventory.entity.UserItem;
import com.coremate.backend.avatar.inventory.repository.UserItemRepository;
import com.coremate.backend.avatar.inventory.service.ItemGrantResult;
import com.coremate.backend.avatar.inventory.service.ItemGrantService;
import com.coremate.backend.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("기본 지급 8종 (2026-08-27 확정)")
class DefaultAvatarItemServiceTest {

    private static final long USER_ID = 7L;

    @Mock
    private AvatarItemRepository avatarItemRepository;
    @Mock
    private UserItemRepository userItemRepository;
    @Mock
    private UserAvatarEquippedItemRepository userAvatarEquippedItemRepository;
    @Mock
    private ItemGrantService itemGrantService;
    @InjectMocks
    private DefaultAvatarItemService defaultAvatarItemService;

    private User user;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        when(user.getUserId()).thenReturn(USER_ID);
        when(userItemRepository.findByUser_UserIdOrderByAcquiredAtDesc(USER_ID)).thenReturn(List.of());
        when(userAvatarEquippedItemRepository.findByUser_UserIdAndSlot(anyLong(), any()))
                .thenReturn(Optional.empty());
    }

    private AvatarItem defaultItem(long itemId, String slot) {
        AvatarItem item = mock(AvatarItem.class);
        when(item.getItemId()).thenReturn(itemId);
        when(item.getSlot()).thenReturn(slot);
        return item;
    }

    private void givenDefaults(AvatarItem... items) {
        when(avatarItemRepository.findByDefaultItemTrueAndActiveTrueOrderByItemIdAsc())
                .thenReturn(List.of(items));
    }

    private void givenGrantSucceeds(long userItemId) {
        UserItem userItem = mock(UserItem.class);
        when(itemGrantService.grant(anyLong(), anyLong(), any(), any()))
                .thenReturn(ItemGrantResult.granted(userItemId));
        when(userItemRepository.findById(userItemId)).thenReturn(Optional.of(userItem));
    }

    private void givenOwns(long itemId) {
        AvatarItem item = mock(AvatarItem.class);
        when(item.getItemId()).thenReturn(itemId);
        UserItem owned = mock(UserItem.class);
        when(owned.isOwned()).thenReturn(true);
        when(owned.getItem()).thenReturn(item);
        when(userItemRepository.findByUser_UserIdOrderByAcquiredAtDesc(USER_ID)).thenReturn(List.of(owned));
    }

    @Nested
    @DisplayName("미보유 기본품을 지급하고 장착한다")
    class Grants {

        @Test
        @DisplayName("DEFAULT_GRANT 경로로 지급하고 슬롯에 장착한다")
        void grantsAndEquips() {
            givenDefaults(defaultItem(10L, "HAIR"));
            givenGrantSucceeds(100L);

            defaultAvatarItemService.ensureGranted(user);

            verify(itemGrantService).grant(eq(USER_ID), eq(10L), eq(AcquiredSource.DEFAULT_GRANT), eq(null));
            ArgumentCaptor<UserAvatarEquippedItem> saved =
                    ArgumentCaptor.forClass(UserAvatarEquippedItem.class);
            verify(userAvatarEquippedItemRepository).save(saved.capture());
            assertThat(saved.getValue().getSlot()).isEqualTo(AvatarSlot.HAIR);
        }

        @Test
        @DisplayName("여러 종을 한 번에 채운다")
        void grantsAllMissing() {
            givenDefaults(defaultItem(10L, "HAIR"), defaultItem(11L, "EYES"), defaultItem(12L, "FLOOR"));
            givenGrantSucceeds(100L);

            defaultAvatarItemService.ensureGranted(user);

            verify(itemGrantService, org.mockito.Mockito.times(3))
                    .grant(anyLong(), anyLong(), eq(AcquiredSource.DEFAULT_GRANT), any());
        }
    }

    @Nested
    @DisplayName("이미 가진 것은 건드리지 않는다")
    class AlreadyOwned {

        // ⚠️ 이게 깨지면 아바타를 볼 때마다 grant() 가 DUPLICATE_REFUNDABLE 을 내고,
        //    호출자가 환급 처리하면 코인이 무한히 새어 나간다.
        @Test
        @DisplayName("보유 중인 기본품에는 grant 를 부르지 않는다")
        void skipsOwned() {
            givenDefaults(defaultItem(10L, "HAIR"));
            givenOwns(10L);

            defaultAvatarItemService.ensureGranted(user);

            verify(itemGrantService, never()).grant(anyLong(), anyLong(), any(), any());
            verify(userAvatarEquippedItemRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("사용자가 바꿔 둔 것을 덮어쓰지 않는다")
    class RespectsUserChoice {

        @Test
        @DisplayName("슬롯이 이미 차 있으면 장착하지 않는다")
        void doesNotOverwriteFilledSlot() {
            givenDefaults(defaultItem(10L, "TOP"));
            givenGrantSucceeds(100L);
            when(userAvatarEquippedItemRepository.findByUser_UserIdAndSlot(USER_ID, AvatarSlot.TOP))
                    .thenReturn(Optional.of(mock(UserAvatarEquippedItem.class)));

            defaultAvatarItemService.ensureGranted(user);

            verify(itemGrantService).grant(anyLong(), anyLong(), any(), any());
            verify(userAvatarEquippedItemRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("아이템 시드 전")
    class NoSeed {

        @Test
        @DisplayName("기본 지급품이 0건이면 아무 조회도 더 하지 않는다")
        void noOpWhenCatalogEmpty() {
            when(avatarItemRepository.findByDefaultItemTrueAndActiveTrueOrderByItemIdAsc())
                    .thenReturn(List.of());

            defaultAvatarItemService.ensureGranted(user);

            verify(userItemRepository, never()).findByUser_UserIdOrderByAcquiredAtDesc(anyLong());
            verify(itemGrantService, never()).grant(anyLong(), anyLong(), any(), any());
        }
    }
}
