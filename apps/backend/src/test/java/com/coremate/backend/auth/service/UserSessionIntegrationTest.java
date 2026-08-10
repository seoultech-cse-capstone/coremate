package com.coremate.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coremate.backend.auth.entity.DevicePlatform;
import com.coremate.backend.auth.entity.SessionRevokeReason;
import com.coremate.backend.auth.entity.UserSession;
import com.coremate.backend.auth.repository.UserSessionRepository;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 기기 세션 통합 테스트(H2).
 *
 * <p>인증 경로에서 Redis 를 걷어냈기 때문에 이 테스트는 별도 인프라 없이 H2 만으로 돈다.
 * 서비스가 트랜잭션을 직접 관리하도록 테스트에는 @Transactional 을 걸지 않고,
 * 만든 세션은 매 테스트 뒤에 지운다.
 */
@SpringBootTest
class UserSessionIntegrationTest {

    private static final Long USER_ID = 91_001L;
    private static final Long OTHER_USER_ID = 91_002L;

    @Autowired
    private UserSessionService userSessionService;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @AfterEach
    void cleanUp() {
        userSessionRepository.deleteAll(userSessionRepository.findByUserId(USER_ID));
        userSessionRepository.deleteAll(userSessionRepository.findByUserId(OTHER_USER_ID));
    }

    @Test
    void 같은_기기로_재로그인하면_세션_식별자만_바뀌고_행은_늘지_않는다() {
        String deviceId = "device-a";

        String firstSessionId = register(deviceId).sessionId();
        String secondSessionId = register(deviceId).sessionId();

        assertThat(secondSessionId).isNotEqualTo(firstSessionId);
        assertThat(userSessionRepository.findByUserId(USER_ID)).hasSize(1);
    }

    @Test
    void 재로그인_후_옛_세션_식별자로는_인증할_수_없다() {
        // 해지된 세션이 같은 기기의 재로그인으로 되살아나면 로그아웃이 무효화된다.
        // 행을 재사용하면서도 부활하지 않는지가 이 설계의 핵심이라 회귀 테스트로 고정한다.
        String deviceId = "device-a";
        String oldSessionId = register(deviceId).sessionId();
        userSessionService.revokeCurrent(USER_ID, oldSessionId, SessionRevokeReason.USER_LOGOUT);

        String newSessionId = register(deviceId).sessionId();

        assertThat(userSessionService.resolveAuthFailure(newSessionId)).isEmpty();
        assertThat(userSessionService.resolveAuthFailure(oldSessionId))
                .contains(ErrorCode.SESSION_EXPIRED);
    }

    @Test
    void 활성_세션이_상한을_넘으면_마지막_접속이_오래된_것부터_해지된다() {
        // 상한만큼 채워 두되 마지막 접속 시각을 명시해 축출 순서를 결정적으로 만든다.
        for (int i = 0; i < UserSessionService.MAX_ACTIVE_SESSIONS_PER_USER; i++) {
            saveSessionSeenMinutesAgo("device-" + i, "session-" + i, 50 - (i * 10));
        }

        String newestSessionId = register("device-new").sessionId();

        List<UserSession> active =
                userSessionRepository.findByUserIdAndRevokedAtIsNullOrderByLastSeenAtDesc(USER_ID);
        assertThat(active).hasSize(UserSessionService.MAX_ACTIVE_SESSIONS_PER_USER);
        assertThat(active).extracting(UserSession::getSessionId).contains(newestSessionId);

        // 50분 전에 마지막으로 접속한 session-0 이 가장 오래된 세션이다.
        UserSession evicted = findBySessionId("session-0");
        assertThat(evicted.isActive()).isFalse();
        assertThat(evicted.getRevokeReason()).isEqualTo(SessionRevokeReason.MAX_SESSION_EXCEEDED);
        assertThat(userSessionService.resolveAuthFailure("session-0"))
                .contains(ErrorCode.SESSION_REVOKED_BY_OTHER_DEVICE);
    }

    @Test
    void 원격_해지된_세션은_사용자_해지_사유로_막힌다() {
        String targetSessionId = register("device-tablet").sessionId();
        String currentSessionId = register("device-phone").sessionId();

        userSessionService.revokeBySessionId(USER_ID, currentSessionId, targetSessionId);

        assertThat(userSessionService.resolveAuthFailure(targetSessionId))
                .contains(ErrorCode.SESSION_REVOKED_BY_USER);
        assertThat(userSessionService.resolveAuthFailure(currentSessionId)).isEmpty();
    }

    @Test
    void 현재_사용_중인_기기는_원격_해지할_수_없다() {
        String currentSessionId = register("device-phone").sessionId();

        assertThatThrownBy(() ->
                userSessionService.revokeBySessionId(USER_ID, currentSessionId, currentSessionId))
                .isInstanceOf(CoreMateException.class)
                .extracting(e -> ((CoreMateException) e).getErrorCode())
                .isEqualTo(ErrorCode.CANNOT_REVOKE_CURRENT_SESSION);

        assertThat(userSessionService.resolveAuthFailure(currentSessionId)).isEmpty();
    }

    @Test
    void 남의_세션은_해지해도_아무_일도_일어나지_않는다() {
        String victimSessionId = register("device-victim").sessionId();

        // 예외 없이 통과해야 한다. 존재 여부를 응답으로 흘리지 않기 위해서다.
        userSessionService.revokeBySessionId(OTHER_USER_ID, "attacker-session", victimSessionId);

        assertThat(userSessionService.resolveAuthFailure(victimSessionId)).isEmpty();
    }

    @Test
    void 전체_해지는_모든_기기를_끊고_안내가_필요없는_사유로_남는다() {
        String phoneSessionId = register("device-phone").sessionId();
        String tabletSessionId = register("device-tablet").sessionId();

        userSessionService.revokeAll(USER_ID, SessionRevokeReason.PASSWORD_RESET);

        // 본인이 이유를 아는 해지이므로 "다른 기기에서 로그인" 안내가 뜨면 안 된다.
        assertThat(userSessionService.resolveAuthFailure(phoneSessionId))
                .contains(ErrorCode.SESSION_EXPIRED);
        assertThat(userSessionService.resolveAuthFailure(tabletSessionId))
                .contains(ErrorCode.SESSION_EXPIRED);
        assertThat(userSessionRepository.findByUserIdAndRevokedAtIsNullOrderByLastSeenAtDesc(USER_ID))
                .isEmpty();
    }

    @Test
    void 기기_식별자를_보내지_않으면_서버가_발급하고_기기마다_다른_행이_된다() {
        UserSessionService.SessionRegistration first = userSessionService.registerOnLogin(
                USER_ID, null, "이름 없는 기기", "IOS", "18.2");
        UserSessionService.SessionRegistration second = userSessionService.registerOnLogin(
                USER_ID, null, "이름 없는 기기", "IOS", "18.2");

        assertThat(first.deviceId()).isNotBlank();
        assertThat(second.deviceId()).isNotEqualTo(first.deviceId());
        assertThat(userSessionRepository.findByUserId(USER_ID)).hasSize(2);
    }

    @Test
    void 모르는_플랫폼_문자열은_로그인을_막지_않고_UNKNOWN_으로_저장된다() {
        String sessionId = userSessionService
                .registerOnLogin(USER_ID, "device-web", "브라우저", "WINDOWS", null)
                .sessionId();

        assertThat(findBySessionId(sessionId).getPlatform()).isEqualTo(DevicePlatform.UNKNOWN);
    }

    @Test
    void sid_클레임이_없는_레거시_토큰은_조용히_재로그인시킨다() {
        assertThat(userSessionService.resolveAuthFailure(null)).contains(ErrorCode.SESSION_EXPIRED);
        assertThat(userSessionService.resolveAuthFailure("")).contains(ErrorCode.SESSION_EXPIRED);
    }

    private UserSessionService.SessionRegistration register(String deviceId) {
        return userSessionService.registerOnLogin(USER_ID, deviceId, "테스트 기기", "IOS", "18.2");
    }

    private void saveSessionSeenMinutesAgo(String deviceId, String sessionId, int minutesAgo) {
        userSessionRepository.save(UserSession.builder()
                .userId(USER_ID)
                .deviceId(deviceId)
                .sessionId(sessionId)
                .deviceName("테스트 기기")
                .platform(DevicePlatform.IOS)
                .osVersion("18.2")
                .lastSeenAt(LocalDateTime.now().minusMinutes(minutesAgo))
                .build());
    }

    private UserSession findBySessionId(String sessionId) {
        Optional<UserSession> session = userSessionRepository.findBySessionId(sessionId);
        assertThat(session).isPresent();
        return session.get();
    }
}
