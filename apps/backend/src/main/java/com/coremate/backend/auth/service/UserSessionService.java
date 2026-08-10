package com.coremate.backend.auth.service;

import com.coremate.backend.auth.entity.DevicePlatform;
import com.coremate.backend.auth.entity.SessionRevokeReason;
import com.coremate.backend.auth.entity.UserSession;
import com.coremate.backend.notification.service.NotificationMessages;
import com.coremate.backend.auth.repository.UserSessionRepository;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기기 세션의 등록·검증·해지를 담당한다.
 *
 * <p><b>주의: 이 클래스의 어떤 메서드도 상태를 바꾼 뒤 예외를 던지지 않는다.</b>
 * 호출부인 AuthService 가 클래스 레벨 {@code @Transactional} 이라, 해지 처리 후 예외를 던지면
 * 해지 자체가 롤백되어 기능이 스스로를 무효화한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserSessionService {

    /**
     * 사용자당 유지할 활성 기기 수.
     *
     * <p>계정 공유를 막으려는 값이 아니라 유령 행을 흡수하기 위한 값이다. 안드로이드는 앱을 지우면
     * SecureStore 가 함께 날아가 재설치마다 새 deviceId 가 생기므로, 상한이 없으면 기기 목록에
     * 유령 행이 무한히 쌓인다. 사용자에게 "5대 제한"으로 노출하지 않는다.
     *
     * <p>{@code @Value} 가 아니라 상수인 이유: src/test/resources/application.yaml 이 main 의
     * application.yaml 을 통째로 대체해서, 새 프로퍼티를 만들면 test yaml 에도 넣어야 하고
     * 빠뜨리면 증상이 "테스트 하나 실패"가 아니라 "@SpringBootTest 전부 실패"다.
     *
     * <p>이 값을 1로 바꾸면 그대로 카카오톡식 단일 세션(last-wins)이 된다.
     */
    public static final int MAX_ACTIVE_SESSIONS_PER_USER = 5;

    private static final String ANONYMOUS_DEVICE_NAME = "알 수 없는 기기";
    private static final int DEVICE_NAME_MAX_LENGTH = 64;

    private final UserSessionRepository userSessionRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** 로그인 응답에 돌려줄 값. 앱은 deviceId 를 저장해 다음 로그인에 다시 보낸다. */
    public record SessionRegistration(String deviceId, String sessionId) {
    }

    /**
     * 로그인 시 기기 세션을 등록한다.
     *
     * <p>같은 기기(=같은 deviceId)면 기존 행을 재사용하고 sessionId 만 새로 발급한다.
     * 그래서 행 수는 기기 수만큼만 늘고, 이전에 해지된 세션이 되살아나지도 않는다.
     */
    public SessionRegistration registerOnLogin(Long userId, String requestedDeviceId, String deviceName,
                                               String platform, String osVersion) {
        LocalDateTime now = LocalDateTime.now();
        String deviceId = normalizeDeviceId(requestedDeviceId);
        String sessionId = UUID.randomUUID().toString();
        String safeDeviceName = normalizeDeviceName(deviceName);
        DevicePlatform devicePlatform = DevicePlatform.from(platform);
        String safeOsVersion = truncate(osVersion, 32);

        // (user_id, device_id) 유니크 제약이 동시 로그인 경합의 방어선이다. 같은 사용자가 같은 기기에서
        // 동시에 두 번 로그인해 제약에 걸리면 트랜잭션이 롤백되고 로그인이 실패한다. 여기서 잡아
        // 복구하지 않는 이유는, 제약 위반이 발생한 트랜잭션은 이미 rollback-only 로 표시되어
        // 같은 트랜잭션 안에서 재조회·재저장을 해봐야 커밋되지 않기 때문이다. 부분 상태를 남기지 않고
        // 실패시키면 사용자가 한 번 더 로그인하는 것으로 스스로 해소된다.
        // 새 기기 로그인 알림 판정은 세션을 만들기 "전"에 해야 한다. 만든 뒤에 세면 방금 만든
        // 행까지 포함돼 항상 아는 기기로 보인다.
        List<UserSession> sessionsBeforeLogin = userSessionRepository.findByUserId(userId);
        boolean knownDevice = sessionsBeforeLogin.stream()
                .anyMatch(existing -> deviceId.equals(existing.getDeviceId()));
        boolean hadOtherDevices = sessionsBeforeLogin.stream()
                .anyMatch(existing -> !deviceId.equals(existing.getDeviceId()));

        UserSession session = userSessionRepository.findByUserIdAndDeviceId(userId, deviceId)
                .map(existing -> {
                    existing.relogin(sessionId, safeDeviceName, devicePlatform, safeOsVersion, now);
                    return existing;
                })
                .orElseGet(() -> userSessionRepository.save(UserSession.builder()
                        .userId(userId)
                        .deviceId(deviceId)
                        .sessionId(sessionId)
                        .deviceName(safeDeviceName)
                        .platform(devicePlatform)
                        .osVersion(safeOsVersion)
                        .lastSeenAt(now)
                        .build()));

        enforceMaxActiveSessions(userId, session.getSessionId(), now);

        // 등록된 기기가 없던 최초 로그인에는 알리지 않는다(FR-NOTI-10). 방금 한 행동을 그대로
        // 되돌려 알릴 뿐이고, 알릴 다른 기기도 없다.
        if (!knownDevice && hadOtherDevices) {
            eventPublisher.publishEvent(NotificationMessages.newDeviceLogin(userId, safeDeviceName));
        }

        return new SessionRegistration(deviceId, session.getSessionId());
    }

    /**
     * 인증 필터용 검사. 통과하면 비어 있고, 막아야 하면 사유 ErrorCode 를 담아 돌려준다.
     *
     * <p>여기서 last_seen_at 을 갱신하지 않는다. 매 요청 쓰기가 되어 부하만 늘고, 목록의
     * "마지막 접속"은 로그인·재발급·기기 목록 조회 시각만으로 충분하다.
     */
    @Transactional(readOnly = true)
    public Optional<ErrorCode> resolveAuthFailure(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            // sid 클레임이 없는 레거시 토큰. 조용히 재로그인시킨다.
            return Optional.of(ErrorCode.SESSION_EXPIRED);
        }
        return userSessionRepository.findBySessionId(sessionId)
                .map(session -> session.isActive()
                        ? Optional.<ErrorCode>empty()
                        : Optional.of(toErrorCode(session.getRevokeReason())))
                .orElseGet(() -> Optional.of(ErrorCode.SESSION_EXPIRED));
    }

    /**
     * 로그인·재발급으로 새로 발급한 refresh 토큰을 세션에 붙인다. 원문이 아니라 SHA-256 해시만 남긴다.
     */
    public void attachRefreshToken(String sessionId, String refreshToken, LocalDateTime refreshExpiresAt) {
        userSessionRepository.findBySessionId(sessionId)
                .ifPresent(session ->
                        session.rotate(sha256(refreshToken), refreshExpiresAt, LocalDateTime.now()));
    }

    /**
     * 토큰 재발급용 조회. 상태를 바꾸기 전에 검증만 하므로 여기서는 예외를 던져도 안전하다.
     */
    public UserSession getForReissue(String sessionId, String refreshToken) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new CoreMateException(ErrorCode.SESSION_EXPIRED);
        }

        UserSession session = userSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.SESSION_EXPIRED));

        if (!session.isActive()) {
            throw new CoreMateException(toErrorCode(session.getRevokeReason()));
        }
        if (session.isRefreshExpired(LocalDateTime.now())) {
            throw new CoreMateException(ErrorCode.SESSION_EXPIRED);
        }
        // 해시 불일치는 세션을 해지하지 않는다. 모바일 httpClient 의 single-flight 누수로 방금 회전된
        // 옛 refresh 토큰이 뒤늦게 도착하는 정상 케이스가 있어서, 재사용 탐지로 처리하면 선량한
        // 사용자를 강제 로그아웃시키는 오탐이 된다.
        if (!sha256(refreshToken).equals(session.getRefreshTokenHash())) {
            throw new CoreMateException(ErrorCode.INVALID_TOKEN);
        }
        return session;
    }

    /** 로그아웃. 이미 해지됐거나 행이 없어도 조용히 넘어간다(멱등). */
    public void revokeCurrent(Long userId, String sessionId, SessionRevokeReason reason) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        userSessionRepository.findBySessionId(sessionId)
                .filter(session -> session.getUserId().equals(userId))
                .ifPresent(session -> session.revoke(reason, LocalDateTime.now()));
    }

    /** 비밀번호 재설정·회원 탈퇴처럼 전 기기를 끊어야 할 때. */
    public void revokeAll(Long userId, SessionRevokeReason reason) {
        LocalDateTime now = LocalDateTime.now();
        userSessionRepository.findByUserIdAndRevokedAtIsNullOrderByLastSeenAtDesc(userId)
                .forEach(session -> session.revoke(reason, now));
    }

    /** 현재 기기를 뺀 나머지 전부 해지. 기기 목록을 관찰용에서 대응용으로 바꾸는 요소다. */
    public void revokeOthers(Long userId, String currentSessionId) {
        LocalDateTime now = LocalDateTime.now();
        userSessionRepository.findByUserIdAndRevokedAtIsNullOrderByLastSeenAtDesc(userId).stream()
                .filter(session -> !session.getSessionId().equals(currentSessionId))
                .forEach(session -> session.revoke(SessionRevokeReason.REMOTE_LOGOUT, now));
    }

    /**
     * 기기 목록에서의 개별 해지.
     *
     * <p>없는 세션·남의 세션·이미 해지된 세션은 전부 조용히 통과시킨다. 목록이 오래되어 이미 사라진
     * 기기를 해지하는 것은 사용자 입장에서 오류가 아니고, 존재 여부를 응답으로 흘리지도 않는다.
     * 현재 사용 중인 기기만 명시적으로 막는다(UI 에도 해지 버튼을 두지 않지만 서버에서 한 번 더).
     */
    public void revokeBySessionId(Long userId, String currentSessionId, String targetSessionId) {
        if (targetSessionId.equals(currentSessionId)) {
            throw new CoreMateException(ErrorCode.CANNOT_REVOKE_CURRENT_SESSION);
        }
        userSessionRepository.findBySessionId(targetSessionId)
                .filter(session -> session.getUserId().equals(userId))
                .ifPresent(session -> session.revoke(SessionRevokeReason.REMOTE_LOGOUT, LocalDateTime.now()));
    }

    /** 탈퇴 시 전 세션 해지 + 표시용 기기명 파기. */
    public void revokeAllAndAnonymize(Long userId) {
        revokeAll(userId, SessionRevokeReason.WITHDRAWAL);
        userSessionRepository.findByUserId(userId).forEach(UserSession::anonymizeDevice);
    }

    /** 기기 목록. 조회 시점에 현재 기기의 마지막 접속 시각을 갱신한다. */
    public List<UserSession> listActive(Long userId, String currentSessionId) {
        List<UserSession> sessions =
                userSessionRepository.findByUserIdAndRevokedAtIsNullOrderByLastSeenAtDesc(userId);
        sessions.stream()
                .filter(session -> session.getSessionId().equals(currentSessionId))
                .findFirst()
                .ifPresent(session -> session.touch(LocalDateTime.now()));
        return sessions;
    }

    public String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 은 모든 JVM 이 제공하므로 실제로는 도달하지 않는다.
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다", e);
        }
    }

    /**
     * 상한을 넘긴 만큼 마지막 접속이 오래된 세션부터 해지한다. 로그인을 막지 않으므로 동시 로그인으로
     * 잠깐 상한을 넘겨도 다음 로그인에 정리될 뿐 무해하다. 방금 만든 세션은 축출 대상에서 제외한다.
     */
    private void enforceMaxActiveSessions(Long userId, String currentSessionId, LocalDateTime now) {
        List<UserSession> active =
                userSessionRepository.findByUserIdAndRevokedAtIsNullOrderByLastSeenAtDesc(userId);
        int excess = active.size() - MAX_ACTIVE_SESSIONS_PER_USER;
        if (excess <= 0) {
            return;
        }

        List<UserSession> evictable = active.stream()
                .filter(session -> !session.getSessionId().equals(currentSessionId))
                .toList();
        for (int i = 0; i < excess && i < evictable.size(); i++) {
            // 목록이 최근 접속 순이므로 뒤에서부터가 가장 오래된 것이다.
            evictable.get(evictable.size() - 1 - i)
                    .revoke(SessionRevokeReason.MAX_SESSION_EXCEEDED, now);
        }
    }

    private ErrorCode toErrorCode(SessionRevokeReason reason) {
        if (reason == null) {
            return ErrorCode.SESSION_EXPIRED;
        }
        return switch (reason) {
            case MAX_SESSION_EXCEEDED -> ErrorCode.SESSION_REVOKED_BY_OTHER_DEVICE;
            case REMOTE_LOGOUT -> ErrorCode.SESSION_REVOKED_BY_USER;
            // 본인이 이유를 아는 해지는 놀라는 안내를 띄우지 않는다.
            case USER_LOGOUT, PASSWORD_RESET, WITHDRAWAL -> ErrorCode.SESSION_EXPIRED;
        };
    }

    private String normalizeDeviceId(String requested) {
        if (requested == null || requested.isBlank() || requested.length() > 64) {
            return UUID.randomUUID().toString();
        }
        return requested;
    }

    /** 기기명은 사용자 기기가 보내는 값이다. 길이를 제한하고 줄바꿈·제어문자를 지운다. */
    private String normalizeDeviceName(String raw) {
        if (raw == null || raw.isBlank()) {
            return ANONYMOUS_DEVICE_NAME;
        }
        String cleaned = raw.replaceAll("\\p{Cntrl}", " ").trim();
        return cleaned.isBlank() ? ANONYMOUS_DEVICE_NAME : truncate(cleaned, DEVICE_NAME_MAX_LENGTH);
    }

    private String truncate(String raw, int maxLength) {
        if (raw == null) {
            return null;
        }
        return raw.length() <= maxLength ? raw : raw.substring(0, maxLength);
    }
}
