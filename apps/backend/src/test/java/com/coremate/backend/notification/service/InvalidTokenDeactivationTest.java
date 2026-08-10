package com.coremate.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.coremate.backend.notification.dto.request.RegisterDeviceTokenRequest;
import com.coremate.backend.notification.entity.NotificationSendLog;
import com.coremate.backend.notification.entity.type.DevicePlatform;
import com.coremate.backend.notification.entity.type.NotificationType;
import com.coremate.backend.notification.entity.type.SendFailReason;
import com.coremate.backend.notification.entity.type.SendStatus;
import com.coremate.backend.notification.repository.DeviceTokenRepository;
import com.coremate.backend.notification.repository.NotificationSendLogRepository;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserRole;
import com.coremate.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

// 발송 결과에 따른 기기 토큰 처리. 실제 FCM 대신 PushSender 를 갈아 끼워
// "이런 응답이 오면 어떻게 되는가"만 검증한다.
@SpringBootTest
@Transactional
class InvalidTokenDeactivationTest {

    @MockitoBean
    private PushSender pushSender;

    @Autowired
    private NotificationSender notificationSender;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private NotificationSendLogRepository notificationSendLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void tokenIsDeactivatedWhenFcmReportsItInvalid() {
        User user = createUser("fcm1@example.com", "fcm1");
        registerToken(user, "dead-token");
        given(pushSender.send(anyString(), anyString(), anyString(), any()))
                .willReturn(PushSendResult.failure(SendFailReason.TOKEN_EXPIRED));

        notificationSender.send(NotificationCommand.of(
                user.getUserId(), NotificationType.FRIEND_POKE, "알림", "콕 찔렀어요"));

        // 무효 토큰으로 계속 시도하지 않도록 내려야 한다.
        assertThat(deviceTokenRepository.findByUser_UserIdAndActiveTrue(user.getUserId())).isEmpty();
        assertThat(lastLog(user).getSendStatus()).isEqualTo(SendStatus.FAILED);
        assertThat(lastLog(user).getFailReason()).isEqualTo(SendFailReason.TOKEN_EXPIRED);
    }

    @Test
    void tokenSurvivesTransientFcmError() {
        User user = createUser("fcm2@example.com", "fcm2");
        registerToken(user, "live-token");
        given(pushSender.send(anyString(), anyString(), anyString(), any()))
                .willReturn(PushSendResult.failure(SendFailReason.FCM_ERROR));

        notificationSender.send(NotificationCommand.of(
                user.getUserId(), NotificationType.FRIEND_POKE, "알림", "콕 찔렀어요"));

        // 일시적 오류로 멀쩡한 토큰을 내리면 그 사용자는 이후 알림을 영영 못 받는다.
        assertThat(deviceTokenRepository.findByUser_UserIdAndActiveTrue(user.getUserId())).hasSize(1);
        assertThat(lastLog(user).getSendStatus()).isEqualTo(SendStatus.FAILED);
    }

    @Test
    void senderKeepsWorkingWhenPushThrows() {
        User user = createUser("fcm3@example.com", "fcm3");
        registerToken(user, "boom-token");
        given(pushSender.send(anyString(), anyString(), anyString(), any()))
                .willThrow(new RuntimeException("FCM down"));

        notificationSender.send(NotificationCommand.of(
                user.getUserId(), NotificationType.FRIEND_POKE, "알림", "콕 찔렀어요"));

        // 전송기가 터져도 알림함에는 남고 실패로 기록돼야 한다.
        assertThat(notificationService.getNotifications(user.getUserId())).hasSize(1);
        assertThat(lastLog(user).getFailReason()).isEqualTo(SendFailReason.FCM_ERROR);
    }

    private void registerToken(User user, String token) {
        notificationService.registerDeviceToken(user.getUserId(), RegisterDeviceTokenRequest.builder()
                .token(token)
                .platform(DevicePlatform.ANDROID)
                .deviceId("device-" + user.getUserId())
                .build());
    }

    private NotificationSendLog lastLog(User user) {
        return notificationSendLogRepository.findAll().stream()
                .filter(log -> log.getUserId().equals(user.getUserId()))
                .reduce((first, second) -> second)
                .orElseThrow();
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
