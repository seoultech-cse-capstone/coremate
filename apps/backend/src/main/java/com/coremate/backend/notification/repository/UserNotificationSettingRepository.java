package com.coremate.backend.notification.repository;

import com.coremate.backend.notification.entity.UserNotificationSetting;
import com.coremate.backend.notification.entity.type.NotificationCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserNotificationSettingRepository extends JpaRepository<UserNotificationSetting, Long> {

    List<UserNotificationSetting> findByUser_UserId(Long userId);

    Optional<UserNotificationSetting> findByUser_UserIdAndCategory(Long userId, NotificationCategory category);

    // 회원 탈퇴.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from UserNotificationSetting s where s.user.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
