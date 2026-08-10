package com.coremate.backend.auth.entity;

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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 회원이 로그인한 기기별 세션.
 *
 * <p>핵심은 {@code deviceId} 와 {@code sessionId} 의 역할 분리다.
 * <ul>
 *   <li>{@code deviceId} — 앱 설치 단위 식별자. (userId, deviceId) 유니크로 <b>행을 재사용</b>하는 키일 뿐,
 *       그 자체가 인가 근거가 아니다. 클라이언트가 보내는 값이라 신뢰하지 않는다.</li>
 *   <li>{@code sessionId} — 로그인할 때마다 새로 발급되어 JWT {@code sid} 클레임에 담기는 값.
 *       <b>해지 판정의 기준 단위</b>다.</li>
 * </ul>
 * 이렇게 나누면 행 수는 기기 수만큼만 늘어나면서(정리 배치 불필요), 해지된 세션이 같은 기기의
 * 재로그인으로 되살아나지 않는다. 폐기 전에 발급된 access token 의 옛 sid 는 어느 행에서도
 * 조회되지 않기 때문이다.
 *
 * <p>해지해도 행을 지우지 않는다. 쫓겨난 기기가 다음 요청을 보냈을 때 {@code revokeReason} 을 읽어
 * 정확한 사유의 안내를 띄워야 하기 때문이다.
 *
 * <p>userId 는 물리 FK 없이 논리 참조로 둔다(2026-08-02 로그 테이블 참조 무결성 관례).
 */
@Entity
@Table(
        name = "user_sessions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_user_sessions_user_id_device_id",
                        columnNames = {"user_id", "device_id"}),
                @UniqueConstraint(
                        name = "uq_user_sessions_session_id",
                        columnNames = {"session_id"})
        },
        indexes = @Index(
                name = "idx_user_sessions_user_id_revoked_at",
                columnList = "user_id, revoked_at")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserSession {

    private static final String ANONYMIZED_DEVICE_NAME = "삭제된 기기";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_session_id")
    private Long userSessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @Column(name = "device_name", nullable = false, length = 64)
    private String deviceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DevicePlatform platform;

    @Column(name = "os_version", length = 32)
    private String osVersion;

    /** 재발급 토큰의 SHA-256 hex. 원문은 저장하지 않는다(NFR-PRIV-04). */
    @Column(name = "refresh_token_hash", length = 64)
    private String refreshTokenHash;

    @Column(name = "refresh_expires_at")
    private LocalDateTime refreshExpiresAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "revoke_reason", length = 30)
    private SessionRevokeReason revokeReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private UserSession(Long userId, String deviceId, String sessionId, String deviceName,
                        DevicePlatform platform, String osVersion, String refreshTokenHash,
                        LocalDateTime refreshExpiresAt, LocalDateTime lastSeenAt) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.sessionId = sessionId;
        this.deviceName = deviceName;
        this.platform = platform;
        this.osVersion = osVersion;
        this.refreshTokenHash = refreshTokenHash;
        this.refreshExpiresAt = refreshExpiresAt;
        this.lastSeenAt = lastSeenAt;
    }

    /**
     * 같은 기기의 재로그인. 행은 그대로 두고 세션 정체성만 새로 발급한다.
     * 해지 상태였더라도 여기서 되살아나지만, sessionId 가 바뀌므로 옛 토큰은 통과하지 못한다.
     */
    public void relogin(String newSessionId, String deviceName, DevicePlatform platform,
                        String osVersion, LocalDateTime now) {
        this.sessionId = newSessionId;
        this.deviceName = deviceName;
        this.platform = platform;
        this.osVersion = osVersion;
        this.refreshTokenHash = null;
        this.refreshExpiresAt = null;
        this.lastSeenAt = now;
        this.revokedAt = null;
        this.revokeReason = null;
    }

    /** 토큰 재발급. sessionId 는 그대로 두고 해시와 만료만 갱신한다. */
    public void rotate(String refreshTokenHash, LocalDateTime refreshExpiresAt, LocalDateTime now) {
        this.refreshTokenHash = refreshTokenHash;
        this.refreshExpiresAt = refreshExpiresAt;
        this.lastSeenAt = now;
    }

    /** 이미 해지된 세션에 다시 호출해도 최초 해지 사유를 덮어쓰지 않는다(멱등). */
    public void revoke(SessionRevokeReason reason, LocalDateTime now) {
        if (this.revokedAt != null) {
            return;
        }
        this.revokedAt = now;
        this.revokeReason = reason;
        this.refreshTokenHash = null;
        this.refreshExpiresAt = null;
    }

    public void touch(LocalDateTime now) {
        this.lastSeenAt = now;
    }

    /** 탈퇴 시 표시용 기기명을 지운다. User.withdraw() 의 개인정보 파기와 짝을 이룬다. */
    public void anonymizeDevice() {
        this.deviceName = ANONYMIZED_DEVICE_NAME;
        this.osVersion = null;
    }

    public boolean isActive() {
        return this.revokedAt == null;
    }

    public boolean isRefreshExpired(LocalDateTime now) {
        return this.refreshExpiresAt == null || this.refreshExpiresAt.isBefore(now);
    }
}
