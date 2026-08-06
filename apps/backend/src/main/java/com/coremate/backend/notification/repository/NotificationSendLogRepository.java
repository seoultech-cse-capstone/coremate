package com.coremate.backend.notification.repository;

import com.coremate.backend.notification.entity.NotificationSendLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationSendLogRepository extends JpaRepository<NotificationSendLog, Long> {
}
