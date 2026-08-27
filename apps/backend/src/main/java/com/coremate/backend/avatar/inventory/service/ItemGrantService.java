package com.coremate.backend.avatar.inventory.service;

import com.coremate.backend.avatar.catalog.AvatarItem;
import com.coremate.backend.avatar.catalog.AvatarItemRepository;
import com.coremate.backend.avatar.catalog.type.AcquiredSource;
import com.coremate.backend.avatar.catalog.type.AvatarItemType;
import com.coremate.backend.avatar.catalog.type.ItemRarity;
import com.coremate.backend.avatar.inventory.entity.UserItem;
import com.coremate.backend.avatar.inventory.repository.UserItemRepository;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// avatar.inventory 가 공개하는 유일한 아이템 지급 API.
// 중복 처리 분기(가구는 수량 증가, 나머지는 코인 환급)를 전부 여기에 넣어
// 상점과 랜덤박스가 item_type / rarity 를 몰라도 되게 한다.
@Service
@RequiredArgsConstructor
public class ItemGrantService {

    private final UserItemRepository userItemRepository;
    private final AvatarItemRepository avatarItemRepository;
    private final UserRepository userRepository;

    // 항상 1개를 지급한다. 랜덤박스는 한 번에 보상이 정확히 하나다.
    // 호출자의 트랜잭션에 참여한다 - 코인 차감과 지급이 같이 롤백돼야 한다.
    @Transactional
    public ItemGrantResult grant(long userId, long itemId, AcquiredSource source, Long sourceId) {
        if (source == null) {
            throw new CoreMateException(ErrorCode.INVALID_INPUT);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.USER_NOT_FOUND));
        AvatarItem item = avatarItemRepository.findByItemIdAndActiveTrue(itemId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.AVATAR_ITEM_NOT_FOUND));

        // 같은 아이템 행을 잠그고 읽는다. 동시에 두 번 열려도 한쪽만 새 행을 만든다.
        Optional<UserItem> existing = userItemRepository.findByUser_UserIdAndItem_ItemId(userId, itemId);
        if (existing.isEmpty()) {
            UserItem saved = userItemRepository.save(
                    new UserItem(user, item, 1, source.name(), sourceId));
            return ItemGrantResult.granted(saved.getUserItemId());
        }

        UserItem owned = existing.get();

        // 행은 있는데 수량이 0이면 보유한 게 아니다. 환급 대상이 아니라 재지급이다.
        if (!owned.isOwned()) {
            owned.addQuantity(1);
            return ItemGrantResult.granted(owned.getUserItemId());
        }

        if (AvatarItemType.from(item.getItemType()).isFurniture()) {
            owned.addQuantity(1);
            return ItemGrantResult.quantityIncreased(owned.getUserItemId());
        }

        return ItemGrantResult.duplicateRefundable(
                ItemRarity.from(item.getRarity()).getRefundCoin());
    }
}
