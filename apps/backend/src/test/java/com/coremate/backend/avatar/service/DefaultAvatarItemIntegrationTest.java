package com.coremate.backend.avatar.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.coremate.backend.avatar.catalog.type.AvatarSlot;
import com.coremate.backend.avatar.dto.response.UserAvatarResponse;
import com.coremate.backend.avatar.equip.UserAvatarEquippedItemRepository;
import com.coremate.backend.avatar.inventory.entity.UserItem;
import com.coremate.backend.avatar.inventory.repository.UserItemRepository;
import com.coremate.backend.user.entity.UserRole;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기본 지급 6종 통합 테스트(H2). 아바타를 처음 조회하는 것만으로 6종이 지급·장착되는지,
 * 다시 조회해도 중복 지급되지 않는지를 실제 JPA 왕복으로 검증한다.
 * avatar_items 는 시드가 SQL 로 들어오는 구조라 여기서도 네이티브 INSERT 로 만든다.
 */
@SpringBootTest
@Transactional
@DisplayName("기본 지급 6종 통합 (2026-08-30 개정)")
class DefaultAvatarItemIntegrationTest {

    // 머리·눈·입·상의·하의 + 방 테마 1종. 신발은 「없음」이 정상이라 기본 지급이 아니고,
    // 벽지·바닥은 WALLPAPER 한 아이템이 metadata 로 벽+바닥을 함께 가리킨다(2026-08-30 개정).
    private static final List<String> DEFAULT_SLOTS =
            List.of("HAIR", "EYES", "MOUTH", "TOP", "BOTTOM", "WALLPAPER");

    @Autowired
    private AvatarService avatarService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserItemRepository userItemRepository;
    @Autowired
    private UserAvatarEquippedItemRepository userAvatarEquippedItemRepository;
    @Autowired
    private EntityManager entityManager;

    private Long userId;

    @BeforeEach
    void seed() {
        User user = User.builder()
                .email("default-item@test.com")
                .passwordHash("hash")
                .name("기본지급")
                .nickname("기본지급테스트")
                .phoneNumber("01000000000")
                .role(UserRole.USER)
                .build();
        userId = userRepository.save(user).getUserId();

        for (String slot : DEFAULT_SLOTS) {
            entityManager.createNativeQuery(
                    "INSERT INTO avatar_items"
                            + " (item_name, item_type, slot, rarity, image_url, is_default, is_active,"
                            + " created_at, updated_at)"
                            + " VALUES (?, ?, ?, 'COMMON', 'x.png', TRUE, TRUE,"
                            + " CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                    .setParameter(1, "기본 " + slot)
                    .setParameter(2, slot)
                    .setParameter(3, slot)
                    .executeUpdate();
        }
        // 팔리는 아이템은 후보가 아니다. 이게 딸려 지급되면 안 된다.
        entityManager.createNativeQuery(
                "INSERT INTO avatar_items"
                        + " (item_name, item_type, slot, rarity, image_url, is_default, is_active,"
                        + " created_at, updated_at)"
                        + " VALUES ('판매용 모자', 'HAT', 'HAT', 'RARE', 'x.png', FALSE, TRUE,"
                        + " CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                .executeUpdate();
        entityManager.flush();
    }

    @Test
    @DisplayName("아바타를 처음 조회하면 6종이 지급되고 자동 장착된다")
    void grantsAndEquipsOnFirstView() {
        UserAvatarResponse response = avatarService.getMyAvatar(userId);
        entityManager.flush();

        List<UserItem> owned = userItemRepository.findByUser_UserIdOrderByAcquiredAtDesc(userId);
        assertThat(owned).hasSize(6);
        assertThat(owned).allSatisfy(item -> {
            assertThat(item.getAcquiredSource()).isEqualTo("DEFAULT_GRANT");
            assertThat(item.getQuantity()).isEqualTo(1);
        });
        assertThat(owned.stream().map(item -> item.getItem().getSlot()))
                .containsExactlyInAnyOrderElementsOf(DEFAULT_SLOTS);

        assertThat(userAvatarEquippedItemRepository.findByUser_UserIdOrderBySlotAsc(userId))
                .hasSize(6);
        assertThat(response.profile().equippedItems()).hasSize(6);
    }

    @Test
    @DisplayName("비매품이 아닌 아이템은 지급하지 않는다")
    void doesNotGrantSellableItems() {
        avatarService.getMyAvatar(userId);
        entityManager.flush();

        assertThat(userItemRepository.findByUser_UserIdOrderByAcquiredAtDesc(userId))
                .extracting(item -> item.getItem().getSlot())
                .doesNotContain("HAT");
    }

    // ⚠️ 이게 깨지면 아바타를 볼 때마다 grant() 가 중복 호출돼 환급 코인이 새어 나간다.
    @Test
    @DisplayName("다시 조회해도 중복 지급되지 않는다")
    void isIdempotent() {
        avatarService.getMyAvatar(userId);
        entityManager.flush();
        avatarService.getMyAvatar(userId);
        avatarService.getMyAvatar(userId);
        entityManager.flush();

        assertThat(userItemRepository.findByUser_UserIdOrderByAcquiredAtDesc(userId)).hasSize(6);
        assertThat(userAvatarEquippedItemRepository.findByUser_UserIdOrderBySlotAsc(userId)).hasSize(6);
    }

    @Test
    @DisplayName("렌더 순서대로 정렬되어 내려온다")
    void equippedItemsAreSortedByRenderOrder() {
        UserAvatarResponse response = avatarService.getMyAvatar(userId);

        List<AvatarSlot> slots = response.profile().equippedItems().stream()
                .map(item -> item.slot())
                .toList();
        // 착용 5종이 렌더 순서(하의-상의-머리-눈-입)로 먼저 오고, renderOrder 가 없는 방 테마가 맨 뒤에 온다.
        assertThat(slots).containsExactly(
                AvatarSlot.BOTTOM, AvatarSlot.TOP, AvatarSlot.HAIR,
                AvatarSlot.EYES, AvatarSlot.MOUTH, AvatarSlot.WALLPAPER);
    }
}
