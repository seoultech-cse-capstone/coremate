package com.coremate.backend.notification.entity;

import com.coremate.backend.notification.entity.type.NotificationCategory;
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 사용자가 카테고리 단위로 켜고 끈 푸시 알림 수신 여부.
// 행이 없으면 수신 허용으로 간주하므로 회원가입 시 미리 만들어 두지 않아도 된다.
@Entity
@Table(
        name = "user_notification_settings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_notification_settings_user_category",
                columnNames = {"user_id", "category"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserNotificationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_setting_id")
    private Long notificationSettingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private NotificationCategory category;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UserNotificationSetting(User user, NotificationCategory category, boolean enabled) {
        this.user = user;
        this.category = category;
        this.enabled = enabled;
    }

    public void changeEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
