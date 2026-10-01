package com.coremate.backend.avatar.equip;

import com.coremate.backend.avatar.catalog.type.AvatarSlot;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAvatarEquippedItemRepository extends JpaRepository<UserAvatarEquippedItem, Long> {

    @EntityGraph(attributePaths = {"userItem", "userItem.item"})
    List<UserAvatarEquippedItem> findByUser_UserIdOrderBySlotAsc(Long userId);

    @EntityGraph(attributePaths = {"userItem", "userItem.item"})
    Optional<UserAvatarEquippedItem> findByUser_UserIdAndSlot(Long userId, AvatarSlot slot);

    // 친구 검색·받은 요청처럼 여러 사람의 장착을 한 번에 읽는다. 사람마다 읽으면 목록 길이만큼 쿼리가 나간다.
    @EntityGraph(attributePaths = {"userItem", "userItem.item"})
    List<UserAvatarEquippedItem> findByUser_UserIdInOrderBySlotAsc(Collection<Long> userIds);
}
