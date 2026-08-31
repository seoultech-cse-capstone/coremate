package com.coremate.backend.avatar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coremate.backend.avatar.catalog.type.AvatarSlot;
import com.coremate.backend.avatar.dto.request.UpdateEquippedItemsRequest;
import com.coremate.backend.avatar.dto.request.UpdateEquippedItemsRequest.EquippedItemSlot;
import com.coremate.backend.avatar.equip.UserAvatarEquippedItemRepository;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserRole;
import com.coremate.backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 장착 API 통합 테스트(H2). 12슬롯 검증과 필수 슬롯 해제 차단(EX-ITEM-007)을
 * 실제 서비스 경로로 확인한다. 기본 지급 8종이 이미 장착된 상태에서 시작한다.
 */
@SpringBootTest
@Transactional
@DisplayName("아바타 장착 12슬롯 (2026-08-27 확정)")
class AvatarEquipIntegrationTest {

    private static final List<String> DEFAULT_SLOTS =
            List.of("HAIR", "EYES", "MOUTH", "TOP", "BOTTOM", "SHOES", "WALLPAPER", "FLOOR");

    @Autowired
    private AvatarService avatarService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserAvatarEquippedItemRepository userAvatarEquippedItemRepository;
    @Autowired
    private EntityManager entityManager;

    private Long userId;

    @BeforeEach
    void seed() {
        User user = User.builder()
                .email("equip@test.com").passwordHash("hash").name("장착")
                .nickname("장착테스트").phoneNumber("01011112222").role(UserRole.USER)
                .build();
        userId = userRepository.save(user).getUserId();

        for (String slot : DEFAULT_SLOTS) {
            entityManager.createNativeQuery(
                    "INSERT INTO avatar_items (item_name, item_type, slot, rarity, image_url,"
                            + " is_default, is_active, created_at, updated_at)"
                            + " VALUES (?, ?, ?, 'COMMON', 'x.png', TRUE, TRUE,"
                            + " CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                    .setParameter(1, "기본 " + slot).setParameter(2, slot).setParameter(3, slot)
                    .executeUpdate();
        }
        entityManager.flush();

        // 기본 지급 8종을 받아 장착된 상태로 만든다.
        avatarService.getMyAvatar(userId);
        entityManager.flush();
    }

    private void update(String slot, Long userItemId) {
        avatarService.updateEquippedItems(userId,
                new UpdateEquippedItemsRequest(List.of(new EquippedItemSlot(slot, userItemId))));
    }

    @Nested
    @DisplayName("필수 슬롯 6개는 해제할 수 없다 (EX-ITEM-007)")
    class RequiredSlots {

        // 비우면 렌더가 깨진다. 교체만 허용한다.
        @ParameterizedTest(name = "{0} 은 해제 요청이 막힌다")
        @ValueSource(strings = {"HAIR", "EYES", "MOUTH", "TOP", "BOTTOM", "WALLPAPER"})
        void cannotUnequipRequiredSlot(String slot) {
            assertThatThrownBy(() -> update(slot, null))
                    .isInstanceOf(CoreMateException.class)
                    .extracting(e -> ((CoreMateException) e).getErrorCode())
                    .isEqualTo(ErrorCode.REQUIRED_AVATAR_SLOT);
        }

        @Test
        @DisplayName("차단되면 장착 상태가 그대로 남는다")
        void equippedStateSurvivesRejection() {
            assertThatThrownBy(() -> update("HAIR", null)).isInstanceOf(CoreMateException.class);

            assertThat(userAvatarEquippedItemRepository.findByUser_UserIdAndSlot(userId, AvatarSlot.HAIR))
                    .isPresent();
        }
    }

    @Nested
    @DisplayName("비필수 슬롯은 해제된다")
    class OptionalSlots {

        @ParameterizedTest(name = "{0} 은 해제된다")
        @ValueSource(strings = {"SHOES", "FLOOR"})
        void canUnequipOptionalSlot(String slot) {
            update(slot, null);
            entityManager.flush();

            assertThat(userAvatarEquippedItemRepository
                    .findByUser_UserIdAndSlot(userId, AvatarSlot.from(slot))).isEmpty();
            assertThat(userAvatarEquippedItemRepository.findByUser_UserIdOrderBySlotAsc(userId))
                    .hasSize(7);
        }
    }

    @Nested
    @DisplayName("슬롯 값 검증")
    class SlotValidation {

        // 폐기된 값(HEAD)이나 가구는 슬롯이 아니다. 500 이 아니라 400 이어야 한다.
        @ParameterizedTest(name = "{0} 은 슬롯이 아니다")
        @ValueSource(strings = {"HEAD", "FURNITURE", "CLOTHES", "  "})
        void rejectsNonSlotValues(String slot) {
            assertThatThrownBy(() -> update(slot, null))
                    .isInstanceOf(CoreMateException.class)
                    .extracting(e -> ((CoreMateException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_AVATAR_SLOT);
        }

        @Test
        @DisplayName("소문자와 공백은 정규화해서 받는다")
        void normalizesInput() {
            update(" shoes ", null);
            entityManager.flush();

            assertThat(userAvatarEquippedItemRepository
                    .findByUser_UserIdAndSlot(userId, AvatarSlot.SHOES)).isEmpty();
        }

        @Test
        @DisplayName("한 요청에 같은 슬롯이 두 번 오면 거부한다")
        void rejectsDuplicateSlotInOneRequest() {
            UpdateEquippedItemsRequest request = new UpdateEquippedItemsRequest(List.of(
                    new EquippedItemSlot("SHOES", null),
                    new EquippedItemSlot("SHOES", null)));

            assertThatThrownBy(() -> avatarService.updateEquippedItems(userId, request))
                    .isInstanceOf(CoreMateException.class)
                    .extracting(e -> ((CoreMateException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_AVATAR_SLOT);
        }
    }
}
