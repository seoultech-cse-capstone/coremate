package com.coremate.backend.notification.entity;

import com.coremate.backend.notification.entity.type.NotificationType;
import com.coremate.backend.notification.entity.type.SendFailReason;
import com.coremate.backend.notification.entity.type.SendStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 푸시 발송 시도의 성공/실패/건너뜀 기록.
// 로그 테이블이므로 물리 외래키를 두지 않고 논리 참조만 한다(reward_logs 등과 동일한 관례).
@Entity
@Table(
        name = "notification_send_logs",
        indexes = {
                @Index(name = "idx_send_logs_user_type_sent", columnList = "user_id,notification_type,sent_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class NotificationSendLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_send_log_id")
    private Long notificationSendLogId;

    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Column(name = "device_token_id")
    private Long deviceTokenId;

    @Enumerated(EnumType.STRING)
    @Column(name = "send_status", nullable = false, length = 20)
    private SendStatus sendStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "fail_reason", length = 50)
    private SendFailReason failReason;

    @CreatedDate
    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    private NotificationSendLog(
            Long notificationId,
            Long userId,
            NotificationType notificationType,
            Long deviceTokenId,
            SendStatus sendStatus,
            SendFailReason failReason
    ) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.notificationType = notificationType;
        this.deviceTokenId = deviceTokenId;
        this.sendStatus = sendStatus;
        this.failReason = failReason;
    }

    public static NotificationSendLog success(Long notificationId, Long userId, NotificationType type, Long deviceTokenId) {
        return new NotificationSendLog(notificationId, userId, type, deviceTokenId, SendStatus.SUCCESS, null);
    }

    public static NotificationSendLog failed(
            Long notificationId, Long userId, NotificationType type, Long deviceTokenId, SendFailReason reason) {
        return new NotificationSendLog(notificationId, userId, type, deviceTokenId, SendStatus.FAILED, reason);
    }

    public static NotificationSendLog skipped(
            Long notificationId, Long userId, NotificationType type, SendFailReason reason) {
        return new NotificationSendLog(notificationId, userId, type, null, SendStatus.SKIPPED, reason);
    }
}
