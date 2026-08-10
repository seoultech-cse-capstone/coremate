package com.coremate.backend.notification.repository;

import com.coremate.backend.notification.entity.Notification;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUser_UserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUser_UserIdAndReadFalse(Long userId);

    Optional<Notification> findByNotificationIdAndUser_UserId(Long notificationId, Long userId);

    // 전체 읽음은 건수가 많을 수 있어 엔티티를 하나씩 올리지 않고 한 번에 갱신한다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.read = true, n.readAt = :readAt "
            + "where n.user.userId = :userId and n.read = false")
    int markAllRead(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);
}
