package com.coremate.backend.avatar.service;

import com.coremate.backend.avatar.catalog.AvatarItem;
import com.coremate.backend.avatar.catalog.type.AvatarItemType;
import com.coremate.backend.avatar.dto.request.UpdateAvatarProfileRequest;
import com.coremate.backend.avatar.dto.request.UpdateEquippedItemsRequest;
import com.coremate.backend.avatar.dto.response.AvatarEquippedItemResponse;
import com.coremate.backend.avatar.dto.response.AvatarItemResponse;
import com.coremate.backend.avatar.dto.response.AvatarProfileResponse;
import com.coremate.backend.avatar.dto.response.UserAvatarResponse;
import com.coremate.backend.avatar.dto.type.AvatarGenderType;
import com.coremate.backend.avatar.equip.UserAvatarEquippedItem;
import com.coremate.backend.avatar.equip.UserAvatarEquippedItemRepository;
import com.coremate.backend.avatar.inventory.entity.UserItem;
import com.coremate.backend.avatar.inventory.repository.UserItemRepository;
import com.coremate.backend.avatar.profile.UserAvatarProfile;
import com.coremate.backend.avatar.profile.UserAvatarProfileRepository;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvatarService {

    private final UserRepository userRepository;
    private final UserAvatarProfileRepository userAvatarProfileRepository;
    private final UserAvatarEquippedItemRepository userAvatarEquippedItemRepository;
    private final UserItemRepository userItemRepository;

    // 내 아바타 화면은 프로필, 현재 장착 아이템, 보유 아이템 마스터 정보를 함께 내려준다.
    @Transactional
    public UserAvatarResponse getMyAvatar(Long userId) {
        UserAvatarProfile profile = getOrCreateProfile(userId);
        return new UserAvatarResponse(
                userId,
                toProfileResponse(profile),
                getOwnedAvatarItems(userId)
        );
    }

    // 친구 기능에서는 보관함 전체가 아니라 현재 장착된 아바타만 보여줄 수 있게 별도 메서드로 둔다.
    @Transactional
    public AvatarProfileResponse getAvatarProfile(Long userId) {
        return toProfileResponse(getOrCreateProfile(userId));
    }

    @Transactional
    public AvatarProfileResponse updateProfile(Long userId, UpdateAvatarProfileRequest request) {
        UserAvatarProfile profile = getOrCreateProfile(userId);
        profile.update(request.avatarGender(), request.baseAvatarType());
        return toProfileResponse(profile);
    }

    @Transactional
    public AvatarProfileResponse updateEquippedItems(Long userId, UpdateEquippedItemsRequest request) {
        User user = findUser(userId);
        getOrCreateProfile(userId);

        Set<String> requestedSlots = new HashSet<>();
        for (UpdateEquippedItemsRequest.EquippedItemSlot equippedItem : request.equippedItems()) {
            String slot = normalizeSlot(equippedItem.slot());
            if (!requestedSlots.add(slot)) {
                throw new CoreMateException(ErrorCode.INVALID_AVATAR_SLOT);
            }

            if (equippedItem.userItemId() == null) {
                unequip(userId, slot);
            } else {
                equip(user, slot, equippedItem.userItemId());
            }
        }

        return toProfileResponse(getOrCreateProfile(userId));
    }

    private void equip(User user, String slot, Long userItemId) {
        UserItem userItem = userItemRepository.findByUserItemIdAndUser_UserId(userItemId, user.getUserId())
                .orElseThrow(() -> new CoreMateException(ErrorCode.USER_ITEM_NOT_FOUND));
        AvatarItem item = userItem.getItem();

        if (!userItem.isOwned() || !item.isEquippable()) {
            throw new CoreMateException(ErrorCode.AVATAR_ITEM_NOT_EQUIPPABLE);
        }
        if (!slot.equals(normalizeSlot(item.getSlot()))) {
            throw new CoreMateException(ErrorCode.INVALID_AVATAR_SLOT);
        }

        UserAvatarEquippedItem equippedItem = userAvatarEquippedItemRepository
                .findByUser_UserIdAndSlot(user.getUserId(), slot)
                .orElseGet(() -> userAvatarEquippedItemRepository.save(
                        new UserAvatarEquippedItem(user, slot, userItem)));
        equippedItem.changeUserItem(userItem);
    }

    private void unequip(Long userId, String slot) {
        userAvatarEquippedItemRepository.findByUser_UserIdAndSlot(userId, slot)
                .ifPresent(userAvatarEquippedItemRepository::delete);
    }

    private UserAvatarProfile getOrCreateProfile(Long userId) {
        User user = findUser(userId);
        return userAvatarProfileRepository.findByUser_UserId(userId)
                .orElseGet(() -> userAvatarProfileRepository.save(new UserAvatarProfile(user)));
    }

    private AvatarProfileResponse toProfileResponse(UserAvatarProfile profile) {
        return new AvatarProfileResponse(
                profile.getUser().getUserId(),
                AvatarGenderType.from(profile.getAvatarGender()),
                profile.getBaseAvatarType(),
                getEquippedItems(profile.getUser().getUserId()),
                profile.getUpdatedAt()
        );
    }

    private List<AvatarEquippedItemResponse> getEquippedItems(Long userId) {
        return userAvatarEquippedItemRepository.findByUser_UserIdOrderBySlotAsc(userId)
                .stream()
                .map(this::toEquippedItemResponse)
                .toList();
    }

    private List<AvatarItemResponse> getOwnedAvatarItems(Long userId) {
        return userItemRepository.findByUser_UserIdOrderByAcquiredAtDesc(userId)
                .stream()
                .filter(UserItem::isOwned)
                .map(UserItem::getItem)
                .map(this::toAvatarItemResponse)
                .toList();
    }

    private AvatarEquippedItemResponse toEquippedItemResponse(UserAvatarEquippedItem equippedItem) {
        UserItem userItem = equippedItem.getUserItem();
        AvatarItem item = userItem.getItem();
        return new AvatarEquippedItemResponse(
                equippedItem.getSlot(),
                userItem.getUserItemId(),
                item.getItemId(),
                item.getItemName(),
                AvatarItemType.from(item.getItemType()),
                item.getRarity(),
                item.getImageUrl()
        );
    }

    private AvatarItemResponse toAvatarItemResponse(AvatarItem item) {
        return new AvatarItemResponse(
                item.getItemId(),
                item.getItemName(),
                AvatarItemType.from(item.getItemType()),
                item.getSlot(),
                item.getRarity(),
                item.getImageUrl(),
                item.getDescription(),
                item.isActive()
        );
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.USER_NOT_FOUND));
    }

    private String normalizeSlot(String slot) {
        if (slot == null || slot.isBlank()) {
            throw new CoreMateException(ErrorCode.INVALID_AVATAR_SLOT);
        }
        return slot.trim().toUpperCase();
    }
}
