package com.coremate.backend.avatar.inventory.service;

import com.coremate.backend.avatar.catalog.AvatarItem;
import com.coremate.backend.avatar.catalog.AvatarItemRepository;
import com.coremate.backend.avatar.inventory.dto.response.UserItemInventoryResponse;
import com.coremate.backend.avatar.inventory.dto.response.UserItemResponse;
import com.coremate.backend.avatar.inventory.entity.UserItem;
import com.coremate.backend.avatar.inventory.repository.UserItemRepository;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.repository.UserRepository;
import java.util.List;
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

    public UserItemInventoryResponse getInventory(Long userId) {
        return new UserItemInventoryResponse(userId, getItems(userId));
    }

    // 사용자가 실제 보유한 아이템만 보관함에 표시한다.
    public List<UserItemResponse> getItems(Long userId) {
        findUser(userId);
        return userItemRepository.findByUser_UserIdOrderByAcquiredAtDesc(userId)
                .stream()
                .filter(UserItem::isOwned)
                .map(this::toUserItemResponse)
                .toList();
    }

    // 상점/랜덤박스/레벨업 보상에서 공통으로 호출할 아이템 지급 진입점.
    @Transactional
    public UserItemResponse grantItem(Long userId, Long itemId, int quantity, String acquiredSource, Long sourceId) {
        return toUserItemResponse(grantItemEntity(userId, itemId, quantity, acquiredSource, sourceId));
    }

    // 지급 후 로그 테이블과 직접 연결해야 하는 흐름에서 엔티티를 그대로 반환한다.
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
