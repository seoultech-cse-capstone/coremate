package com.coremate.backend.avatar.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.coremate.backend.avatar.catalog.AvatarItem;
import com.coremate.backend.avatar.catalog.type.AvatarItemType;
import com.coremate.backend.avatar.catalog.type.AvatarSlot;
import com.coremate.backend.avatar.catalog.type.ItemRarity;
import com.coremate.backend.avatar.dto.request.SaveRoomPlacementsRequest;
import com.coremate.backend.avatar.dto.request.UpdateEquippedItemsRequest;
import com.coremate.backend.avatar.dto.request.UpdateEquippedItemsRequest.EquippedItemSlot;
import com.coremate.backend.avatar.dto.request.SaveRoomPlacementsRequest.Placement;
import com.coremate.backend.avatar.inventory.dto.response.InventoryGroupResponse;
import com.coremate.backend.avatar.inventory.dto.response.InventoryItemResponse;
import com.coremate.backend.avatar.inventory.dto.response.InventoryResponse;
import com.coremate.backend.avatar.inventory.entity.UserItem;
import com.coremate.backend.avatar.inventory.repository.UserItemRepository;
import com.coremate.backend.avatar.inventory.service.InventoryService;
import com.coremate.backend.avatar.profile.UserAvatarProfile;
import com.coremate.backend.avatar.profile.UserAvatarProfileRepository;
import com.coremate.backend.avatar.service.AvatarService;
import com.coremate.backend.avatar.service.RoomService;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserRole;
import com.coremate.backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 아이템 보관함 조회 통합 테스트(H2). 13부위 분류, 장착 표시,
 * 가구의 보유/배치/남은 개수, 기본 지급 비매품 표시를 확인한다.
 */
@SpringBootTest
@Transactional
@DisplayName("아이템 보관함 조회 (FR-ITEM-01)")
class InventoryQueryIntegrationTest {

    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private AvatarService avatarService;
    @Autowired
    private RoomService roomService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserItemRepository userItemRepository;
    @Autowired
    private UserAvatarProfileRepository userAvatarProfileRepository;
    @Autowired
    private EntityManager entityManager;

    private User user;
    private Long userId;
    private Long chairUserItemId;
    private Long hairUserItemId;

    @BeforeEach
    void seed() {
        user = userRepository.save(User.builder()
                .email("inv@test.com").passwordHash("h").name("보관함")
                .nickname("보관함테스트").phoneNumber("01012123434").role(UserRole.USER).build());
        userId = user.getUserId();
        userAvatarProfileRepository.save(new UserAvatarProfile(user));

        // 기본 지급 2종(비매품) + 판매용 모자 + 가구(의자 3개 보유)
        hairUserItemId = own(item("기본 머리", "HAIR", "HAIR", true, null), 1);
        own(item("기본 벽지", "WALLPAPER", "WALLPAPER", true, null), 1);
        own(item("판매 모자", "HAT", "HAT", false, null), 1);
        chairUserItemId = own(item("의자", "FURNITURE", null, false,
                "{\"footprint\":{\"w\":1,\"h\":1}}"), 3);
        entityManager.flush();
    }

    private Long item(String name, String type, String slot, boolean isDefault, String metadata) {
        String metaExpr = metadata == null ? "NULL" : "? FORMAT JSON";
        var q = entityManager.createNativeQuery(
                "INSERT INTO avatar_items (item_name, item_type, slot, rarity, image_url,"
                        + " is_default, metadata, is_active, created_at, updated_at)"
                        + " VALUES (?, ?, ?, 'COMMON', 'x.png', ?, " + metaExpr + ", TRUE,"
                        + " CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                .setParameter(1, name).setParameter(2, type).setParameter(3, slot)
                .setParameter(4, isDefault);
        if (metadata != null) {
            q.setParameter(5, metadata);
        }
        q.executeUpdate();
        return ((Number) entityManager.createNativeQuery(
                "SELECT item_id FROM avatar_items WHERE item_name = ?")
                .setParameter(1, name).getSingleResult()).longValue();
    }

    private Long own(Long itemId, int quantity) {
        return userItemRepository.save(new UserItem(
                user, entityManager.find(AvatarItem.class, itemId), quantity, "RANDOM_BOX", null))
                .getUserItemId();
    }

    private InventoryGroupResponse group(InventoryResponse inv, AvatarItemType type) {
        return inv.groups().stream().filter(g -> g.itemType() == type).findFirst().orElseThrow();
    }

    @Nested
    @DisplayName("부위별로 묶어서 내려준다")
    class Grouping {

        @Test
        @DisplayName("13부위가 항상 다 내려간다 - 비어 있어도 필터 칩을 그릴 수 있다")
        void alwaysReturnsThirteenGroups() {
            InventoryResponse inv = inventoryService.getInventory(userId);

            assertThat(inv.groups()).hasSize(13);
            assertThat(inv.groups()).extracting(InventoryGroupResponse::itemType)
                    .containsExactly(AvatarItemType.values());
            assertThat(group(inv, AvatarItemType.EYEWEAR).items()).isEmpty();
            assertThat(group(inv, AvatarItemType.EYEWEAR).count()).isZero();
        }

        @Test
        @DisplayName("총 보유 개수와 부위별 개수가 맞는다")
        void countsMatch() {
            InventoryResponse inv = inventoryService.getInventory(userId);

            assertThat(inv.totalCount()).isEqualTo(4);
            assertThat(group(inv, AvatarItemType.HAIR).count()).isEqualTo(1);
            assertThat(group(inv, AvatarItemType.FURNITURE).count()).isEqualTo(1);
        }

        @Test
        @DisplayName("장착형은 슬롯이 있고 가구는 null 이다")
        void slotIsNullForFurniture() {
            InventoryResponse inv = inventoryService.getInventory(userId);

            assertThat(group(inv, AvatarItemType.HAIR).slot()).isEqualTo(AvatarSlot.HAIR);
            assertThat(group(inv, AvatarItemType.FURNITURE).slot()).isNull();
        }
    }

    @Nested
    @DisplayName("비매품과 장착 여부")
    class Flags {

        @Test
        @DisplayName("기본 지급품은 비매품으로 표시된다")
        void marksDefaultItems() {
            InventoryResponse inv = inventoryService.getInventory(userId);

            assertThat(group(inv, AvatarItemType.HAIR).items().get(0).defaultItem()).isTrue();
            assertThat(group(inv, AvatarItemType.HAT).items().get(0).defaultItem()).isFalse();
        }

        // 1-4 의 자동 장착은 "방금 지급한 것"만 대상이라, 이미 보유한 아이템에는 걸리지 않는다.
        // 여기서는 그 동작이 아니라 equipped 플래그 매핑만 보는 것이라 직접 장착시킨다.
        @Test
        @DisplayName("장착 중인 아이템에 표시가 붙는다")
        void marksEquipped() {
            avatarService.updateEquippedItems(userId, new UpdateEquippedItemsRequest(
                    List.of(new EquippedItemSlot("HAIR", hairUserItemId))));
            entityManager.flush();

            InventoryResponse inv = inventoryService.getInventory(userId);

            assertThat(group(inv, AvatarItemType.HAIR).items().get(0).equipped()).isTrue();
            assertThat(group(inv, AvatarItemType.HAT).items().get(0).equipped()).isFalse();
        }

        @Test
        @DisplayName("등급을 enum 으로 내려준다")
        void exposesRarityAsEnum() {
            InventoryResponse inv = inventoryService.getInventory(userId);

            assertThat(group(inv, AvatarItemType.HAT).items().get(0).rarity())
                    .isEqualTo(ItemRarity.COMMON);
        }
    }

    @Nested
    @DisplayName("가구는 보유·배치·남은 개수를 함께 준다")
    class FurnitureCounts {

        @Test
        @DisplayName("아무것도 안 놓았으면 배치 0, 남은 개수는 보유 수량과 같다")
        void nothingPlaced() {
            InventoryItemResponse chair = group(inventoryService.getInventory(userId),
                    AvatarItemType.FURNITURE).items().get(0);

            assertThat(chair.quantity()).isEqualTo(3);
            assertThat(chair.placedCount()).isZero();
            assertThat(chair.remainingCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("2개를 놓으면 남은 개수가 1이 된다")
        void reflectsPlacedCount() {
            roomService.savePlacements(userId, new SaveRoomPlacementsRequest(List.of(
                    new Placement(chairUserItemId, 0, 0), new Placement(chairUserItemId, 1, 0))));
            entityManager.flush();

            InventoryItemResponse chair = group(inventoryService.getInventory(userId),
                    AvatarItemType.FURNITURE).items().get(0);

            assertThat(chair.quantity()).isEqualTo(3);
            assertThat(chair.placedCount()).isEqualTo(2);
            assertThat(chair.remainingCount()).isEqualTo(1);
        }

        // 가구는 슬롯에 장착하지 않는다. equipped 를 배치와 헷갈리면 안 된다.
        @Test
        @DisplayName("가구는 배치해도 장착으로 표시되지 않는다")
        void furnitureIsNeverEquipped() {
            roomService.savePlacements(userId, new SaveRoomPlacementsRequest(List.of(
                    new Placement(chairUserItemId, 0, 0))));
            entityManager.flush();

            InventoryItemResponse chair = group(inventoryService.getInventory(userId),
                    AvatarItemType.FURNITURE).items().get(0);

            assertThat(chair.equipped()).isFalse();
        }

        @Test
        @DisplayName("장착형은 배치 개수가 null 이다")
        void wearablesHaveNoPlacementCounts() {
            InventoryItemResponse hair = group(inventoryService.getInventory(userId),
                    AvatarItemType.HAIR).items().get(0);

            assertThat(hair.placedCount()).isNull();
            assertThat(hair.remainingCount()).isNull();
        }
    }
}
