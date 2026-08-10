package com.coremate.backend.notification.entity;

import com.coremate.backend.notification.entity.type.NotificationCategory;
import com.coremate.backend.notification.entity.type.NotificationTargetType;
import com.coremate.backend.notification.entity.type.NotificationType;
import com.coremate.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 사용자에게 전달된 알림을 앱 안에서 다시 확인할 수 있도록 저장한다.
// 푸시 도달 여부와 무관하게 생성하되, 수신을 거부한 카테고리는 아예 만들지 않는다.
// 끈 알림이 목록에 나타나면 설정이 동작하지 않은 것으로 보이기 때문이다.
@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_user_read", columnList = "user_id,is_read"),
                @Index(name = "idx_notifications_user_created", columnList = "user_id,created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private NotificationCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "body", nullable = false, length = 255)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 30)
    private NotificationTargetType targetType;

    // 종류에 따라 참조 대상이 달라지는 다형성 참조라 물리 외래키를 두지 않는다.
    // 대상이 삭제된 경우 앱은 이동 없이 목록만 표시한다.
    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public Notification(
            User user,
            NotificationType notificationType,
            String title,
            String body,
            NotificationTargetType targetType,
            Long targetId
    ) {
        this.user = user;
        this.category = notificationType.getCategory();
        this.notificationType = notificationType;
        this.title = title;
        this.body = body;
        this.targetType = targetType == null ? NotificationTargetType.NONE : targetType;
        this.targetId = targetId;
        this.read = false;
    }

    public void markRead(LocalDateTime readAt) {
        if (this.read) {
            return;
        }
        this.read = true;
        this.readAt = readAt;
    }
}
