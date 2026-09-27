package com.coremate.backend.notification.repository;

import com.coremate.backend.notification.entity.UserNotificationSetting;
import com.coremate.backend.notification.entity.type.NotificationCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserNotificationSettingRepository extends JpaRepository<UserNotificationSetting, Long> {

    List<UserNotificationSetting> findByUser_UserId(Long userId);

    Optional<UserNotificationSetting> findByUser_UserIdAndCategory(Long userId, NotificationCategory category);
}
