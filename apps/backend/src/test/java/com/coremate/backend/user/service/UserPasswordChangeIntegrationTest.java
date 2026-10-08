package com.coremate.backend.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coremate.backend.auth.entity.SessionRevokeReason;
import com.coremate.backend.auth.entity.UserSession;
import com.coremate.backend.auth.repository.UserSessionRepository;
import com.coremate.backend.auth.service.UserSessionService;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.user.dto.request.ChangePasswordRequest;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserRole;
import com.coremate.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 상태 비밀번호 변경 통합 테스트(H2, #250). 바꾸면 지금 기기만 남고 다른 기기는
 * 안내 없이 끊기는지, 현재 비밀번호가 틀리면 아무 기기도 끊기지 않는지 확인한다.
 */
@SpringBootTest
@Transactional
class UserPasswordChangeIntegrationTest {

    private static final String OLD_PASSWORD = "OldP@ssw0rd!";
    private static final String NEW_PASSWORD = "N3wP@ssw0rd!";

    @Autowired
    private UserService userService;
    @Autowired
    private UserSessionService userSessionService;
    @Autowired
    private UserSessionRepository userSessionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;

    @BeforeEach
    void seed() {
        user = userRepository.save(User.builder()
                .email("password-change@test.com")
                .passwordHash(passwordEncoder.encode(OLD_PASSWORD))
                .nickname("비번변경")
                .role(UserRole.USER)
                .build());
    }

    @Test
    void 비밀번호를_바꾸면_지금_기기만_남고_다른_기기는_안내_없이_끊긴다() {
        String phoneSessionId = register("device-phone");
        String tabletSessionId = register("device-tablet");

        userService.changePassword(user.getUserId(), phoneSessionId, request(OLD_PASSWORD));

        assertThat(passwordEncoder.matches(NEW_PASSWORD, reloadUser().getPasswordHash())).isTrue();
        assertThat(userSessionService.resolveAuthFailure(phoneSessionId)).isEmpty();
        // 본인이 한 변경이므로 "다른 기기에서 로그인"·"기기 관리에서 해지" 안내가 뜨면 안 된다.
        assertThat(userSessionService.resolveAuthFailure(tabletSessionId))
                .contains(ErrorCode.SESSION_EXPIRED);
        assertThat(userSessionRepository.findBySessionId(tabletSessionId))
                .get()
                .extracting(UserSession::getRevokeReason)
                .isEqualTo(SessionRevokeReason.PASSWORD_CHANGE);
    }

    @Test
    void 현재_비밀번호가_틀리면_어느_기기도_끊기지_않는다() {
        String phoneSessionId = register("device-phone");
        String tabletSessionId = register("device-tablet");

        assertThatThrownBy(() ->
                userService.changePassword(user.getUserId(), phoneSessionId, request("Wr0ngP@ss!")))
                .isInstanceOf(CoreMateException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PASSWORD);

        assertThat(passwordEncoder.matches(OLD_PASSWORD, reloadUser().getPasswordHash())).isTrue();
        assertThat(userSessionService.resolveAuthFailure(phoneSessionId)).isEmpty();
        assertThat(userSessionService.resolveAuthFailure(tabletSessionId)).isEmpty();
    }

    private String register(String deviceId) {
        return userSessionService
                .registerOnLogin(user.getUserId(), deviceId, "테스트 기기", "IOS", "18.2")
                .sessionId();
    }

    private ChangePasswordRequest request(String currentPassword) {
        return ChangePasswordRequest.builder()
                .currentPassword(currentPassword)
                .newPassword(NEW_PASSWORD)
                .build();
    }

    private User reloadUser() {
        return userRepository.findById(user.getUserId()).orElseThrow();
    }
}
