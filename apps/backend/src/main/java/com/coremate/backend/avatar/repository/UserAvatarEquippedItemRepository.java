package com.coremate.backend.avatar.repository;

import com.coremate.backend.avatar.entity.UserAvatarEquippedItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAvatarEquippedItemRepository extends JpaRepository<UserAvatarEquippedItem, Long> {

    @EntityGraph(attributePaths = {"userItem", "userItem.item"})
    List<UserAvatarEquippedItem> findByUser_UserIdOrderBySlotAsc(Long userId);

    @EntityGraph(attributePaths = {"userItem", "userItem.item"})
    Optional<UserAvatarEquippedItem> findByUser_UserIdAndSlot(Long userId, String slot);
}
