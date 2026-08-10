package com.coremate.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.coremate.backend.notification.dto.request.RegisterDeviceTokenRequest;
import com.coremate.backend.notification.dto.request.UpdateNotificationSettingRequest;
import com.coremate.backend.notification.entity.NotificationSendLog;
import com.coremate.backend.notification.entity.type.DevicePlatform;
import com.coremate.backend.notification.entity.type.NotificationCategory;
import com.coremate.backend.notification.entity.type.NotificationType;
import com.coremate.backend.notification.entity.type.SendFailReason;
import com.coremate.backend.notification.entity.type.SendStatus;
import com.coremate.backend.notification.repository.DeviceTokenRepository;
import com.coremate.backend.notification.repository.NotificationSendLogRepository;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserRole;
import com.coremate.backend.user.repository.UserRepository;
import com.coremate.backend.notification.entity.DeviceToken;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class NotificationSenderTest {

    @Autowired
    private NotificationSender notificationSender;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationSendLogRepository notificationSendLogRepository;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void notificationIsStoredAndSentWhenTokenExists() {
        User user = createUser("noti1@example.com", "noti1");
        registerToken(user, "token-noti1");

        notificationSender.send(command(user, "콕 찔렀어요"));

        assertThat(notificationService.getNotifications(user.getUserId())).hasSize(1);
        assertThat(notificationService.getUnreadCount(user.getUserId())).isEqualTo(1);
        assertThat(lastLog(user).getSendStatus()).isEqualTo(SendStatus.SUCCESS);
    }

    @Test
    void notificationIsNotStoredWhenCategoryDisabled() {
        User user = createUser("noti2@example.com", "noti2");
        registerToken(user, "token-noti2");
        notificationService.updateSetting(user.getUserId(), UpdateNotificationSettingRequest.builder()
                .category(NotificationCategory.FRIEND)
                .enabled(false)
                .build());

        notificationSender.send(command(user, "콕 찔렀어요"));

        // 끈 카테고리는 알림함에도 남기지 않는다. 목록에 보이면 설정이 동작하지 않은 것으로 읽힌다.
        assertThat(notificationService.getNotifications(user.getUserId())).isEmpty();

        NotificationSendLog log = lastLog(user);
        assertThat(log.getSendStatus()).isEqualTo(SendStatus.SKIPPED);
        assertThat(log.getFailReason()).isEqualTo(SendFailReason.OPTED_OUT);
    }

    @Test
    void notificationIsStoredButSkippedWhenNoActiveToken() {
        User user = createUser("noti3@example.com", "noti3");

        notificationSender.send(command(user, "콕 찔렀어요"));

        // 토큰이 없어도 알림함에는 남아야 한다. 나중에 앱을 열면 확인할 수 있어야 하기 때문이다.
        assertThat(notificationService.getNotifications(user.getUserId())).hasSize(1);

        NotificationSendLog log = lastLog(user);
        assertThat(log.getSendStatus()).isEqualTo(SendStatus.SKIPPED);
        assertThat(log.getFailReason()).isEqualTo(SendFailReason.NO_TOKEN);
    }

    @Test
    void securityNotificationIgnoresUserSetting() {
        User user = createUser("noti4@example.com", "noti4");
        registerToken(user, "token-noti4");

        // SYSTEM 은 설정 대상이 아니라 끌 수 없다. 다른 카테고리를 모두 꺼도 발송된다.
        notificationService.updateSetting(user.getUserId(), UpdateNotificationSettingRequest.builder()
                .category(NotificationCategory.FRIEND)
                .enabled(false)
                .build());

        notificationSender.send(new NotificationCommand(
                user.getUserId(), NotificationType.NEW_DEVICE_LOGIN, "새 기기 로그인", "iPhone 에서 로그인했습니다", null, null));

        assertThat(notificationService.getNotifications(user.getUserId())).hasSize(1);
        assertThat(lastLog(user).getSendStatus()).isEqualTo(SendStatus.SUCCESS);
    }

    @Test
    void tokenIsRefreshedInsteadOfDuplicatedForSameDevice() {
        User user = createUser("noti5@example.com", "noti5");
        registerToken(user, "token-old");
        registerToken(user, "token-new");

        List<DeviceToken> tokens =
                deviceTokenRepository.findByUser_UserIdAndActiveTrue(user.getUserId());

        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getToken()).isEqualTo("token-new");
    }

    private NotificationCommand command(User user, String body) {
        return NotificationCommand.of(user.getUserId(), NotificationType.FRIEND_POKE, "알림", body);
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
