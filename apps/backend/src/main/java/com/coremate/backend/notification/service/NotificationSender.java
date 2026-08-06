package com.coremate.backend.notification.service;

import com.coremate.backend.notification.entity.DeviceToken;
import com.coremate.backend.notification.entity.Notification;
import com.coremate.backend.notification.entity.NotificationSendLog;
import com.coremate.backend.notification.entity.UserNotificationSetting;
import com.coremate.backend.notification.entity.type.NotificationCategory;
import com.coremate.backend.notification.entity.type.SendFailReason;
import com.coremate.backend.notification.repository.DeviceTokenRepository;
import com.coremate.backend.notification.repository.NotificationRepository;
import com.coremate.backend.notification.repository.NotificationSendLogRepository;
import com.coremate.backend.notification.repository.UserNotificationSettingRepository;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 알림 발송 공통 처리(FR-NOTI-04).
// 수신 설정 확인 → 알림함 저장 → 푸시 발송 → 발송 로그 기록을 한 곳에서 처리한다.
// 알림을 보내는 기능들은 이 클래스만 주입받으면 되고, 각자 수신 설정이나 토큰을 다루지 않는다.
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSender {

    private final NotificationRepository notificationRepository;
    private final NotificationSendLogRepository notificationSendLogRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final UserNotificationSettingRepository userNotificationSettingRepository;
    private final UserRepository userRepository;
    private final PushSender pushSender;

    // 발송에 실패해도 예외를 밖으로 던지지 않는다. 알림은 부가 기능이라, 콕 찌르기나 출석
    // 처리 같은 원래 동작이 알림 때문에 실패하면 안 된다(FR-NOTI-04 완료 기준).
    @Transactional
    public void send(NotificationCommand command) {
        try {
            doSend(command);
        } catch (Exception e) {
            log.error("알림 발송 처리 실패. userId={} type={}", command.userId(), command.type(), e);
        }
    }

    private void doSend(NotificationCommand command) {
        NotificationCategory category = command.type().getCategory();

        if (!isCategoryEnabled(command.userId(), category)) {
            // 수신을 거부한 카테고리는 알림함에도 남기지 않는다. 끈 알림이 목록에 나타나면
            // 설정이 동작하지 않은 것으로 보인다.
            notificationSendLogRepository.save(NotificationSendLog.skipped(
                    null, command.userId(), command.type(), SendFailReason.OPTED_OUT));
            return;
        }

        User user = userRepository.findById(command.userId()).orElse(null);
        if (user == null) {
            log.warn("알림 수신자를 찾을 수 없어 발송을 건너뛴다. userId={}", command.userId());
            return;
        }

        Notification notification = notificationRepository.save(new Notification(
                user,
                command.type(),
                command.title(),
                command.body(),
                command.targetType(),
                command.targetId()
        ));

        List<DeviceToken> tokens = deviceTokenRepository.findByUser_UserIdAndActiveTrue(command.userId());
        if (tokens.isEmpty()) {
            // 앱을 아직 설치하지 않았거나 알림 권한을 거부한 사용자. 알림함에는 남는다.
            notificationSendLogRepository.save(NotificationSendLog.skipped(
                    notification.getNotificationId(), command.userId(), command.type(), SendFailReason.NO_TOKEN));
            return;
        }

        Map<String, String> data = buildData(command, notification);
        for (DeviceToken token : tokens) {
            pushToToken(token, notification, command, data);
        }
    }

    private void pushToToken(
            DeviceToken token, Notification notification, NotificationCommand command, Map<String, String> data) {
        PushSendResult result;
        try {
            result = pushSender.send(token.getToken(), command.title(), command.body(), data);
        } catch (Exception e) {
            log.error("푸시 전송 중 오류. deviceTokenId={}", token.getDeviceTokenId(), e);
            result = PushSendResult.failure(SendFailReason.FCM_ERROR);
        }

        if (result.successful()) {
            token.markUsed(LocalDateTime.now());
            notificationSendLogRepository.save(NotificationSendLog.success(
                    notification.getNotificationId(), command.userId(), command.type(), token.getDeviceTokenId()));
            return;
        }

        if (result.tokenInvalid()) {
            // 더 이상 유효하지 않은 토큰으로 계속 시도하지 않도록 내려둔다.
            token.deactivate();
        }
        notificationSendLogRepository.save(NotificationSendLog.failed(
                notification.getNotificationId(),
                command.userId(),
                command.type(),
                token.getDeviceTokenId(),
                result.failReason()
        ));
    }

    // 앱이 알림을 눌렀을 때 어디로 이동할지 판단하는 데 쓴다.
    private Map<String, String> buildData(NotificationCommand command, Notification notification) {
        Map<String, String> data = new HashMap<>();
        data.put("notificationId", String.valueOf(notification.getNotificationId()));
        data.put("type", command.type().name());
        data.put("targetType", notification.getTargetType().name());
        if (command.targetId() != null) {
            data.put("targetId", String.valueOf(command.targetId()));
        }
        return data;
    }

    // 저장된 설정이 없으면 수신 허용으로 본다. 계정 보안 알림은 설정 대상이 아니라 항상 보낸다.
    private boolean isCategoryEnabled(Long userId, NotificationCategory category) {
        if (!category.isConfigurable()) {
            return true;
        }
        return userNotificationSettingRepository.findByUser_UserIdAndCategory(userId, category)
                .map(UserNotificationSetting::isEnabled)
                .orElse(true);
    }
}
