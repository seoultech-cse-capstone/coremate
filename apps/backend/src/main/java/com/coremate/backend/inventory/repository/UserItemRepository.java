package com.coremate.backend.inventory.repository;

import com.coremate.backend.inventory.entity.UserItem;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface UserItemRepository extends JpaRepository<UserItem, Long> {

    @EntityGraph(attributePaths = "item")
    List<UserItem> findByUser_UserIdOrderByAcquiredAtDesc(Long userId);

    @EntityGraph(attributePaths = "item")
    Optional<UserItem> findByUserItemIdAndUser_UserId(Long userItemId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "item")
    Optional<UserItem> findByUser_UserIdAndItem_ItemId(Long userId, Long itemId);
}
