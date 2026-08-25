package com.coremate.backend.notification.entity;

import com.coremate.backend.notification.entity.type.DevicePlatform;
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
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 푸시 알림 발송 대상 기기. 인증에 쓰는 user_sessions(기기 세션)와는 목적이 다른 별개
// 테이블이며, 이 토큰은 인증 근거로 사용하지 않는다.
@Entity
@Table(
        name = "device_tokens",
        indexes = {
                @Index(name = "idx_device_tokens_user_active", columnList = "user_id,is_active"),
                @Index(name = "uk_device_tokens_token", columnList = "token", unique = true)
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_token_id")
    private Long deviceTokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false, length = 255)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    private DevicePlatform platform;

    // user_sessions.device_id 와 같은 값을 논리적으로 참조한다. 물리 외래키는 두지 않는다.
    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public DeviceToken(User user, String token, DevicePlatform platform, String deviceId) {
        this.user = user;
        this.token = token;
        this.platform = platform;
        this.deviceId = deviceId;
        this.active = true;
    }

    // 같은 사용자·기기 조합에 새 토큰이 오면 행을 새로 만들지 않고 갱신한다.
    public void refresh(String token, DevicePlatform platform) {
        this.token = token;
        this.platform = platform;
        this.active = true;
    }

    /**
     * 이미 존재하는 토큰 행을 지금 등록하는 사용자·기기 것으로 넘겨받는다.
     *
     * <p>token 에 UNIQUE 제약이 있어, 같은 토큰을 가진 행을 두고 새로 INSERT 하면 중복 키로 실패한다.
     * 기기를 다른 계정에 물려주거나 앱을 지웠다 깔아 기기 식별자만 새로 생긴 경우가 여기 해당한다.
     */
    public void takeOver(User user, DevicePlatform platform, String deviceId) {
        this.user = user;
        this.platform = platform;
        if (deviceId != null && !deviceId.isBlank()) {
            this.deviceId = deviceId;
        }
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void markUsed(LocalDateTime usedAt) {
        this.lastUsedAt = usedAt;
    }
}
