package com.coremate.backend.avatar.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coremate.backend.avatar.catalog.AvatarItem;
import com.coremate.backend.avatar.catalog.AvatarItemRepository;
import com.coremate.backend.avatar.catalog.type.AcquiredSource;
import com.coremate.backend.avatar.inventory.entity.UserItem;
import com.coremate.backend.avatar.inventory.repository.UserItemRepository;
import com.coremate.backend.avatar.inventory.service.ItemGrantResult.Outcome;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("아이템 지급 (2026-08-27 확정)")
class ItemGrantServiceTest {

    private static final long USER_ID = 1L;
    private static final long ITEM_ID = 10L;
    private static final long SOURCE_ID = 77L;
    private static final long NEW_ROW_ID = 500L;

    @Mock
    private UserItemRepository userItemRepository;
    @Mock
    private AvatarItemRepository avatarItemRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private ItemGrantService itemGrantService;

    private User user;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userItemRepository.save(any(UserItem.class))).thenAnswer(call -> {
            UserItem saved = call.getArgument(0);
            ReflectionTestUtils.setField(saved, "userItemId", NEW_ROW_ID);
            return saved;
        });
    }

    private AvatarItem givenItem(String itemType, String rarity) {
        AvatarItem item = mock(AvatarItem.class);
        when(item.getItemType()).thenReturn(itemType);
        when(item.getRarity()).thenReturn(rarity);
        when(avatarItemRepository.findByItemIdAndActiveTrue(ITEM_ID)).thenReturn(Optional.of(item));
        return item;
    }

    private UserItem givenOwned(AvatarItem item, int quantity) {
        UserItem owned = new UserItem(user, item, quantity, AcquiredSource.RANDOM_BOX.name(), null);
        ReflectionTestUtils.setField(owned, "userItemId", 200L);
        when(userItemRepository.findByUser_UserIdAndItem_ItemId(USER_ID, ITEM_ID))
                .thenReturn(Optional.of(owned));
        return owned;
    }

    private void givenNotOwned() {
        when(userItemRepository.findByUser_UserIdAndItem_ItemId(USER_ID, ITEM_ID))
                .thenReturn(Optional.empty());
    }

    private ItemGrantResult grant() {
        return itemGrantService.grant(USER_ID, ITEM_ID, AcquiredSource.RANDOM_BOX, SOURCE_ID);
    }

    @Nested
    @DisplayName("미보유였으면 새 행을 만든다")
    class FirstTime {

        @Test
        @DisplayName("GRANTED 를 주고 수량 1, 획득 경로를 그대로 저장한다")
        void grantsNewRow() {
            givenItem("HAIR", "RARE");
            givenNotOwned();

            ItemGrantResult result = grant();

            assertThat(result.outcome()).isEqualTo(Outcome.GRANTED);
            assertThat(result.userItemId()).isEqualTo(NEW_ROW_ID);
            assertThat(result.refundCoin()).isZero();

            ArgumentCaptor<UserItem> saved = ArgumentCaptor.forClass(UserItem.class);
            verify(userItemRepository).save(saved.capture());
            assertThat(saved.getValue().getQuantity()).isEqualTo(1);
            assertThat(saved.getValue().getAcquiredSource()).isEqualTo("RANDOM_BOX");
            assertThat(saved.getValue().getSourceId()).isEqualTo(SOURCE_ID);
        }

        @Test
        @DisplayName("행은 있는데 수량이 0이면 환급이 아니라 재지급이다")
        void zeroQuantityRowIsNotADuplicate() {
            AvatarItem item = givenItem("HAIR", "EPIC");
            UserItem owned = givenOwned(item, 0);

            ItemGrantResult result = grant();

            assertThat(result.outcome()).isEqualTo(Outcome.GRANTED);
            assertThat(result.refundCoin()).isZero();
            assertThat(owned.getQuantity()).isEqualTo(1);
            verify(userItemRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("FURNITURE 중복은 수량을 늘린다")
    class FurnitureDuplicate {

        @Test
        @DisplayName("QUANTITY_INCREASED 를 주고 환급하지 않는다")
        void increasesQuantity() {
            AvatarItem furniture = givenItem("FURNITURE", "COMMON");
            UserItem owned = givenOwned(furniture, 2);

            ItemGrantResult result = grant();

            assertThat(result.outcome()).isEqualTo(Outcome.QUANTITY_INCREASED);
            assertThat(result.userItemId()).isEqualTo(200L);
            assertThat(result.refundCoin()).isZero();
            assertThat(result.isRefundable()).isFalse();
            assertThat(owned.getQuantity()).isEqualTo(3);
            verify(userItemRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("그 밖의 중복은 코인으로 환급한다")
    class RefundableDuplicate {

        @ParameterizedTest(name = "{0} 등급 중복은 {1} 코인")
        @CsvSource({"COMMON, 20", "RARE, 50", "EPIC, 100", "LEGENDARY, 200"})
        @DisplayName("등급별 환급액은 호출자가 계산하지 않는다")
        void refundsByRarity(String rarity, int expectedCoin) {
            AvatarItem item = givenItem("HAIR", rarity);
            UserItem owned = givenOwned(item, 1);

            ItemGrantResult result = grant();

            assertThat(result.outcome()).isEqualTo(Outcome.DUPLICATE_REFUNDABLE);
            assertThat(result.isRefundable()).isTrue();
            assertThat(result.refundCoin()).isEqualTo(expectedCoin);
            assertThat(result.userItemId()).isNull();
            // user_items 는 손대지 않는다
            assertThat(owned.getQuantity()).isEqualTo(1);
            verify(userItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("벽지도 가구가 아니므로 환급 대상이다")
        void wallpaperIsRefundable() {
            givenOwned(givenItem("WALLPAPER", "COMMON"), 1);

            assertThat(grant().outcome()).isEqualTo(Outcome.DUPLICATE_REFUNDABLE);
        }
    }

    @Nested
    @DisplayName("막아야 하는 요청")
    class Rejected {

        @Test
        @DisplayName("획득 경로가 없으면 예외")
        void nullSource() {
            assertThatThrownBy(() -> itemGrantService.grant(USER_ID, ITEM_ID, null, SOURCE_ID))
                    .isInstanceOf(CoreMateException.class);
            verify(userItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("비활성이거나 없는 아이템이면 예외")
        void inactiveItem() {
            when(avatarItemRepository.findByItemIdAndActiveTrue(ITEM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(ItemGrantServiceTest.this::grant).isInstanceOf(CoreMateException.class);
            verify(userItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("없는 사용자면 예외 - 아이템을 찾기 전에 걸린다")
        void unknownUser() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(ItemGrantServiceTest.this::grant).isInstanceOf(CoreMateException.class);
            verify(avatarItemRepository, never()).findByItemIdAndActiveTrue(any());
        }
    }
}
