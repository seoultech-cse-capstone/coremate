package com.coremate.backend.avatar.service;

import com.coremate.backend.avatar.catalog.AvatarItem;
import com.coremate.backend.avatar.catalog.type.AvatarItemType;
import com.coremate.backend.avatar.catalog.type.AvatarSlot;
import com.coremate.backend.avatar.dto.request.UpdateEquippedItemsRequest;
import com.coremate.backend.avatar.dto.response.AvatarEquippedItemResponse;
import com.coremate.backend.avatar.dto.response.AvatarItemResponse;
import com.coremate.backend.avatar.dto.response.AvatarProfileResponse;
import com.coremate.backend.avatar.dto.response.UserAvatarResponse;
import com.coremate.backend.avatar.equip.UserAvatarEquippedItem;
import com.coremate.backend.avatar.equip.UserAvatarEquippedItemRepository;
import com.coremate.backend.avatar.inventory.entity.UserItem;
import com.coremate.backend.avatar.inventory.repository.UserItemRepository;
import com.coremate.backend.avatar.profile.RoomPoseElapsed;
import com.coremate.backend.avatar.profile.UserAvatarProfile;
import com.coremate.backend.avatar.profile.UserAvatarProfileRepository;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
    private final DefaultAvatarItemService defaultAvatarItemService;

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

    // 친구 목록처럼 여러 사람의 아바타 프로필을 한 번에 읽는다(#322). 사람마다 getAvatarProfile 을 부르면
    // 목록 길이만큼 쿼리가 나가고, 쓰기 경로(프로필 생성·기본 아이템 지급)가 섞여 커넥션을 길게 쥔다.
    // 여기서는 생성·지급 없이 지금 상태만 읽는다 — 프로필이 없는 사람은 기본값으로 돌려주고(앱이 기본 모습을
    // 그린다), 남을 목록에서 봤다고 그 사람의 프로필이 생기지 않게 한다.
    public Map<Long, AvatarProfileResponse> getAvatarProfilesByUser(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<AvatarEquippedItemResponse>> equippedByUser = getEquippedItemsByUser(userIds);
        Map<Long, UserAvatarProfile> profileByUser = userAvatarProfileRepository.findByUser_UserIdIn(userIds)
                .stream()
                .collect(Collectors.toMap(profile -> profile.getUser().getUserId(), profile -> profile));
        Map<Long, AvatarProfileResponse> result = new LinkedHashMap<>();
        for (Long userId : userIds) {
            List<AvatarEquippedItemResponse> equippedItems = equippedByUser.getOrDefault(userId, List.of());
            UserAvatarProfile profile = profileByUser.get(userId);
            result.put(userId, profile != null
                    ? toProfileResponse(profile, equippedItems)
                    : defaultProfileResponse(userId, equippedItems));
        }
        return result;
    }

    // 프로필이 아직 없는 사용자의 기본 응답. 베이스 마네킹만 두고 포즈 정보는 비운다.
    private AvatarProfileResponse defaultProfileResponse(Long userId,
            List<AvatarEquippedItemResponse> equippedItems) {
        return new AvatarProfileResponse(userId, UserAvatarProfile.DEFAULT_BASE_AVATAR_TYPE,
                equippedItems, null, null, null, null);
    }

    @Transactional
    public AvatarProfileResponse updateEquippedItems(Long userId, UpdateEquippedItemsRequest request) {
        User user = findUser(userId);
        getOrCreateProfile(userId);

        Set<AvatarSlot> requestedSlots = new HashSet<>();
        for (UpdateEquippedItemsRequest.EquippedItemSlot equippedItem : request.equippedItems()) {
            // 슬롯 문자열은 여기서 12종 중 하나로 좁힌다. 아니면 INVALID_AVATAR_SLOT.
            AvatarSlot slot = AvatarSlot.from(equippedItem.slot());
            if (!requestedSlots.add(slot)) {
                throw new CoreMateException(ErrorCode.INVALID_AVATAR_SLOT);
            }

            if (equippedItem.userItemId() == null) {
                // 필수 6종(머리·눈·입·상의·하의·방 테마)은 비우면 렌더가 깨지므로 교체만 허용한다. EX-ITEM-007
                if (slot.isRequired()) {
                    throw new CoreMateException(ErrorCode.REQUIRED_AVATAR_SLOT);
                }
                unequip(userId, slot);
            } else {
                equip(user, slot, equippedItem.userItemId());
            }
        }

        return toProfileResponse(getOrCreateProfile(userId));
    }

    private void equip(User user, AvatarSlot slot, Long userItemId) {
        UserItem userItem = userItemRepository.findByUserItemIdAndUser_UserId(userItemId, user.getUserId())
                .orElseThrow(() -> new CoreMateException(ErrorCode.USER_ITEM_NOT_FOUND));
        AvatarItem item = userItem.getItem();

        if (!userItem.isOwned() || !item.isEquippable()) {
            throw new CoreMateException(ErrorCode.AVATAR_ITEM_NOT_EQUIPPABLE);
        }
        if (slot != AvatarSlot.from(item.getSlot())) {
            throw new CoreMateException(ErrorCode.INVALID_AVATAR_SLOT);
        }

        UserAvatarEquippedItem equippedItem = userAvatarEquippedItemRepository
                .findByUser_UserIdAndSlot(user.getUserId(), slot)
                .orElseGet(() -> userAvatarEquippedItemRepository.save(
                        new UserAvatarEquippedItem(user, slot, userItem)));
        equippedItem.changeUserItem(userItem);
    }

    private void unequip(Long userId, AvatarSlot slot) {
        userAvatarEquippedItemRepository.findByUser_UserIdAndSlot(userId, slot)
                .ifPresent(userAvatarEquippedItemRepository::delete);
    }

    // 아바타를 보는 모든 경로가 여기를 지난다. 기본 지급 6종 보충도 같은 자리에서 한다.
    private UserAvatarProfile getOrCreateProfile(Long userId) {
        User user = findUser(userId);
        UserAvatarProfile profile = userAvatarProfileRepository.findByUser_UserId(userId)
                .orElseGet(() -> userAvatarProfileRepository.save(new UserAvatarProfile(user)));
        defaultAvatarItemService.ensureGranted(user);
        return profile;
    }

    private AvatarProfileResponse toProfileResponse(UserAvatarProfile profile) {
        return toProfileResponse(profile, getEquippedItems(profile.getUser().getUserId()));
    }

    private AvatarProfileResponse toProfileResponse(UserAvatarProfile profile,
            List<AvatarEquippedItemResponse> equippedItems) {
        return new AvatarProfileResponse(
                profile.getUser().getUserId(),
                profile.getBaseAvatarType(),
                equippedItems,
                profile.getRoomPose(),
                profile.getRoomPoseUpdatedAt(),
                RoomPoseElapsed.label(profile.getRoomPose(), profile.getRoomPoseUpdatedAt(),
                        LocalDateTime.now()),
                profile.getUpdatedAt()
        );
    }

    // 목록 화면(친구 검색·받은 요청)의 아이콘용. 프로필을 만들거나 기본 아이템을 채우지 않고 지금 장착만 읽는다.
    // 장착이 하나도 없는 사람은 결과에 없으니 호출부가 빈 목록으로 다룬다(앱은 빠진 부위를 기본 모습으로 그린다).
    public Map<Long, List<AvatarEquippedItemResponse>> getEquippedItemsByUser(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userAvatarEquippedItemRepository.findByUser_UserIdInOrderBySlotAsc(userIds)
                .stream()
                .collect(Collectors.groupingBy(
                        equipped -> equipped.getUser().getUserId(),
                        Collectors.collectingAndThen(Collectors.toList(), this::toRenderOrderedResponses)));
    }

    private List<AvatarEquippedItemResponse> getEquippedItems(Long userId) {
        return toRenderOrderedResponses(userAvatarEquippedItemRepository.findByUser_UserIdOrderBySlotAsc(userId));
    }

    private List<AvatarEquippedItemResponse> toRenderOrderedResponses(List<UserAvatarEquippedItem> equippedItems) {
        // 렌더 순서는 서버가 갖는다. 앱이 따로 순서를 적어두면 둘이 어긋난다.
        // 스택에 없는 슬롯(PET/WALLPAPER/FLOOR)은 뒤로 밀고, 그 안에서는 레포지토리의 slot 순서를 유지한다.
        return equippedItems.stream()
                .map(this::toEquippedItemResponse)
                .sorted(Comparator.comparing(AvatarEquippedItemResponse::renderOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
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
                item.getImageUrl(),
                equippedItem.getSlot().getRenderOrder()
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

}
