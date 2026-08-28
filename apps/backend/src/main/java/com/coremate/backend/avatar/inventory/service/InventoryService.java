package com.coremate.backend.avatar.inventory.service;

import com.coremate.backend.avatar.catalog.AvatarItem;
import com.coremate.backend.avatar.catalog.AvatarItemRepository;
import com.coremate.backend.avatar.catalog.ItemMetadataReader;
import com.coremate.backend.avatar.catalog.type.AvatarItemType;
import com.coremate.backend.avatar.catalog.type.AvatarSlot;
import com.coremate.backend.avatar.catalog.type.ItemRarity;
import com.coremate.backend.avatar.equip.UserAvatarEquippedItem;
import com.coremate.backend.avatar.equip.UserAvatarEquippedItemRepository;
import com.coremate.backend.avatar.inventory.dto.response.InventoryGroupResponse;
import com.coremate.backend.avatar.inventory.dto.response.InventoryItemResponse;
import com.coremate.backend.avatar.inventory.dto.response.InventoryResponse;
import com.coremate.backend.avatar.inventory.dto.response.UserItemResponse;
import com.coremate.backend.avatar.inventory.entity.UserItem;
import com.coremate.backend.avatar.inventory.repository.UserItemRepository;
import com.coremate.backend.avatar.room.UserRoomPlacement;
import com.coremate.backend.avatar.room.UserRoomPlacementRepository;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final UserItemRepository userItemRepository;
    private final AvatarItemRepository avatarItemRepository;
    private final UserRepository userRepository;
    private final UserAvatarEquippedItemRepository userAvatarEquippedItemRepository;
    private final UserRoomPlacementRepository userRoomPlacementRepository;
    private final ItemMetadataReader itemMetadataReader;

    /**
     * 보유 아이템을 부위별로 묶어 내려준다. 13종을 항상 다 내려보내므로
     * 아이템이 없는 부위도 필터 칩을 그릴 수 있다.
     * 가구는 장착이 아니라 배치라, 방에 놓인 개수와 남은 개수를 함께 준다.
     */
    public InventoryResponse getInventory(Long userId) {
        findUser(userId);

        List<UserItem> owned = userItemRepository.findByUser_UserIdOrderByAcquiredAtDesc(userId)
                .stream()
                .filter(UserItem::isOwned)
                .toList();

        Set<Long> equippedUserItemIds = new HashSet<>();
        for (UserAvatarEquippedItem equipped
                : userAvatarEquippedItemRepository.findByUser_UserIdOrderBySlotAsc(userId)) {
            equippedUserItemIds.add(equipped.getUserItem().getUserItemId());
        }

        Map<Long, Integer> placedByUserItemId = new HashMap<>();
        for (UserRoomPlacement placement
                : userRoomPlacementRepository.findByUser_UserIdOrderByGridYAscGridXAsc(userId)) {
            placedByUserItemId.merge(placement.getUserItem().getUserItemId(), 1, Integer::sum);
        }

        Map<AvatarItemType, List<InventoryItemResponse>> byType = new HashMap<>();
        for (UserItem userItem : owned) {
            AvatarItemType itemType = AvatarItemType.from(userItem.getItem().getItemType());
            byType.computeIfAbsent(itemType, key -> new ArrayList<>())
                    .add(toInventoryItem(userItem, itemType, equippedUserItemIds, placedByUserItemId));
        }

        List<InventoryGroupResponse> groups = new ArrayList<>();
        for (AvatarItemType itemType : AvatarItemType.values()) {
            List<InventoryItemResponse> items = byType.getOrDefault(itemType, List.of());
            groups.add(new InventoryGroupResponse(
                    itemType, AvatarSlot.of(itemType), items.size(), items));
        }
        return new InventoryResponse(userId, owned.size(), groups);
    }

    private InventoryItemResponse toInventoryItem(UserItem userItem, AvatarItemType itemType,
                                                  Set<Long> equippedUserItemIds,
                                                  Map<Long, Integer> placedByUserItemId) {
        AvatarItem item = userItem.getItem();
        // 가구는 슬롯에 장착하지 않고 좌표로 배치한다. 그래서 개수 두 개를 따로 준다.
        boolean furniture = itemType.isFurniture();
        Integer placed = furniture
                ? placedByUserItemId.getOrDefault(userItem.getUserItemId(), 0)
                : null;
        return new InventoryItemResponse(
                userItem.getUserItemId(),
                item.getItemId(),
                item.getItemName(),
                itemType,
                AvatarSlot.of(itemType),
                ItemRarity.from(item.getRarity()),
                item.getImageUrl(),
                item.isDefaultItem(),
                !furniture && equippedUserItemIds.contains(userItem.getUserItemId()),
                userItem.getQuantity(),
                placed,
                furniture ? userItem.getQuantity() - placed : null,
                itemMetadataReader.read(item),
                userItem.getAcquiredAt()
        );
    }

    // 상점/랜덤박스/레벨업 보상에서 공통으로 호출할 아이템 지급 진입점.
    // @deprecated 중복 처리 분기가 없어 가구가 아닌 아이템도 수량만 늘린다.
    // ItemGrantService.grant() 로 옮긴다. 상점이 이전을 마치면 지운다.
    @Deprecated(forRemoval = true)
    @Transactional
    public UserItemResponse grantItem(Long userId, Long itemId, int quantity, String acquiredSource, Long sourceId) {
        return toUserItemResponse(grantItemEntity(userId, itemId, quantity, acquiredSource, sourceId));
    }

    // 지급 후 로그 테이블과 직접 연결해야 하는 흐름에서 엔티티를 그대로 반환한다.
    // @deprecated 위와 같은 이유. ItemGrantService.grant() 가 userItemId 를 돌려준다.
    @Deprecated(forRemoval = true)
    @Transactional
    public UserItem grantItemEntity(Long userId, Long itemId, int quantity, String acquiredSource, Long sourceId) {
        validateGrantRequest(quantity, acquiredSource);
        User user = findUser(userId);
        AvatarItem item = avatarItemRepository.findByItemIdAndActiveTrue(itemId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.AVATAR_ITEM_NOT_FOUND));

        return userItemRepository.findByUser_UserIdAndItem_ItemId(userId, itemId)
                .map(existing -> {
                    existing.addQuantity(quantity);
                    return existing;
                })
                .orElseGet(() -> userItemRepository.save(new UserItem(
                        user,
                        item,
                        quantity,
                        acquiredSource,
                        sourceId
                )));
    }

    private UserItemResponse toUserItemResponse(UserItem userItem) {
        AvatarItem item = userItem.getItem();
        return new UserItemResponse(
                userItem.getUserItemId(),
                userItem.getUser().getUserId(),
                item.getItemId(),
                item.getItemName(),
                item.getItemType(),
                item.getSlot(),
                item.getRarity(),
                item.getImageUrl(),
                userItem.getQuantity(),
                userItem.getAcquiredSource(),
                userItem.getAcquiredAt()
        );
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateGrantRequest(int quantity, String acquiredSource) {
        if (quantity <= 0 || acquiredSource == null || acquiredSource.isBlank()) {
            throw new CoreMateException(ErrorCode.INVALID_INPUT);
        }
    }
}
