package com.coremate.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.coremate.backend.notification.dto.request.RegisterDeviceTokenRequest;
import com.coremate.backend.notification.entity.DeviceToken;
import com.coremate.backend.notification.entity.type.DevicePlatform;
import com.coremate.backend.notification.repository.DeviceTokenRepository;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserRole;
import com.coremate.backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기기 토큰 등록이 몇 번을 불러도 안전한지 검증한다(FR-NOTI-02 의 "갱신").
 *
 * <p>token 컬럼에 UNIQUE 제약이 있는데 등록이 항상 INSERT 였다. 그래서 앱을 두 번째로 켜는
 * 순간부터 같은 토큰으로 500(Duplicate entry)이 났다. 화면상으로는 홈에서 알 수 없지만
 * 기기 토큰이 갱신되지 않아 알림이 끊길 수 있다.
 */
@SpringBootTest
@Transactional
@DisplayName("기기 토큰 등록 멱등성")
class DeviceTokenRegistrationIdempotencyTest {

    private static final String TOKEN = "ExponentPushToken[same-token]";

    @Autowired private NotificationService notificationService;
    @Autowired private DeviceTokenRepository deviceTokenRepository;
    @Autowired private UserRepository userRepository;

    @Test
    @DisplayName("같은 사용자·기기로 여러 번 등록해도 행이 늘지 않는다")
    void repeatedRegistrationKeepsSingleRow() {
        User user = createUser("idem1@example.com", "idem1");

        register(user, TOKEN, "device-A");
        register(user, TOKEN, "device-A");
        register(user, TOKEN, "device-A");

        assertThat(deviceTokenRepository.findByUser_UserIdAndActiveTrue(user.getUserId())).hasSize(1);
    }

    @Test
    @DisplayName("토큰은 같은데 기기 식별자만 바뀌어도 실패하지 않고 갱신된다")
    void sameTokenWithNewDeviceIdUpdatesInPlace() {
        User user = createUser("idem2@example.com", "idem2");

        register(user, TOKEN, "device-A");
        // 앱을 지웠다 깔면 푸시 토큰은 그대로인데 기기 식별자만 새로 생길 수 있다.
        // 예전에는 이 경로가 곧바로 Duplicate entry 로 터졌다.
        register(user, TOKEN, "device-B");

        DeviceToken saved = deviceTokenRepository.findByToken(TOKEN).orElseThrow();
        assertThat(saved.getDeviceId()).isEqualTo("device-B");
        assertThat(deviceTokenRepository.findByUser_UserIdAndActiveTrue(user.getUserId())).hasSize(1);
    }

    @Test
    @DisplayName("다른 계정이 쓰던 토큰이면 새 사용자가 넘겨받고 이전 사용자에게는 안 간다")
    void tokenIsTakenOverByTheNewOwner() {
        User previous = createUser("idem3@example.com", "idem3");
        User next = createUser("idem4@example.com", "idem4");

        register(previous, TOKEN, "device-A");
        register(next, TOKEN, "device-A");

        assertThat(deviceTokenRepository.findByUser_UserIdAndActiveTrue(previous.getUserId())).isEmpty();
        assertThat(deviceTokenRepository.findByUser_UserIdAndActiveTrue(next.getUserId())).hasSize(1);
        assertThat(deviceTokenRepository.findByToken(TOKEN).orElseThrow()
                .getUser().getUserId()).isEqualTo(next.getUserId());
    }

    private void register(User user, String token, String deviceId) {
        notificationService.registerDeviceToken(user.getUserId(), RegisterDeviceTokenRequest.builder()
                .token(token)
                .platform(DevicePlatform.IOS)
                .deviceId(deviceId)
                .build());
    }

    private User createUser(String email, String nickname) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash("{noop}password")
                .nickname(nickname)
                .role(UserRole.USER)
                .build());
    }
}
