package com.coremate.backend.avatar.service;

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
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.user.entity.User;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 기본 지급 8종(머리·눈·입·상의·하의·신발·벽지·바닥)을 채워 넣는다.
// 아바타 프로필을 조회하는 자리에서 부르므로 신규 가입자는 처음 볼 때 받고,
// 기본 지급이 생기기 전에 가입한 사용자도 같은 자리에서 보충된다(FR-AVATAR-02).
// 회원가입 코드를 건드리지 않는 이유가 이것이다.
@Service
@RequiredArgsConstructor
public class DefaultAvatarItemService {

    private final AvatarItemRepository avatarItemRepository;
    private final UserItemRepository userItemRepository;
    private final UserAvatarEquippedItemRepository userAvatarEquippedItemRepository;
    private final ItemGrantService itemGrantService;

    @Transactional
    public void ensureGranted(User user) {
        List<AvatarItem> defaults = avatarItemRepository.findByDefaultItemTrueAndActiveTrueOrderByItemIdAsc();
        if (defaults.isEmpty()) {
            // 아이템 마스터 시드가 아직 없는 단계에서는 할 일이 없다.
            return;
        }

        Set<Long> ownedItemIds = ownedItemIds(user.getUserId());
        for (AvatarItem item : defaults) {
            if (ownedItemIds.contains(item.getItemId())) {
                // ⚠️ 이미 가진 것에 grant() 를 다시 부르면 DUPLICATE_REFUNDABLE 이 떨어진다.
                // 호출자가 그걸 환급으로 처리하면 아바타를 볼 때마다 코인이 새어 나간다.
                continue;
            }
            grantAndEquip(user, item);
        }
    }

    private Set<Long> ownedItemIds(Long userId) {
        Set<Long> ids = new HashSet<>();
        for (UserItem userItem : userItemRepository.findByUser_UserIdOrderByAcquiredAtDesc(userId)) {
            if (userItem.isOwned()) {
                ids.add(userItem.getItem().getItemId());
            }
        }
        return ids;
    }

    private void grantAndEquip(User user, AvatarItem item) {
        ItemGrantResult result = itemGrantService.grant(
                user.getUserId(), item.getItemId(), AcquiredSource.DEFAULT_GRANT, null);

        // 방금 지급한 것만 자동 장착한다. 사용자가 나중에 벗어 둔 슬롯을 다시 채우지 않는다.
        AvatarSlot slot = AvatarSlot.from(item.getSlot());
        if (userAvatarEquippedItemRepository.findByUser_UserIdAndSlot(user.getUserId(), slot).isPresent()) {
            return;
        }
        UserItem userItem = userItemRepository.findById(result.userItemId())
                .orElseThrow(() -> new CoreMateException(ErrorCode.USER_ITEM_NOT_FOUND));
        userAvatarEquippedItemRepository.save(new UserAvatarEquippedItem(user, slot, userItem));
    }
}
