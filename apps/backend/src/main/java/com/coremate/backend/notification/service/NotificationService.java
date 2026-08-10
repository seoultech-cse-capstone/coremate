package com.coremate.backend.notification.service;

import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.notification.dto.request.RegisterDeviceTokenRequest;
import com.coremate.backend.notification.dto.request.UpdateNotificationSettingRequest;
import com.coremate.backend.notification.dto.response.NotificationResponse;
import com.coremate.backend.notification.dto.response.NotificationSettingResponse;
import com.coremate.backend.notification.entity.DeviceToken;
import com.coremate.backend.notification.entity.Notification;
import com.coremate.backend.notification.entity.UserNotificationSetting;
import com.coremate.backend.notification.entity.type.NotificationCategory;
import com.coremate.backend.notification.repository.DeviceTokenRepository;
import com.coremate.backend.notification.repository.NotificationRepository;
import com.coremate.backend.notification.repository.UserNotificationSettingRepository;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserStatus;
import com.coremate.backend.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 사용자가 직접 호출하는 알림 API. 발송 파이프라인은 NotificationSender 가 담당한다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    // 알림함은 90일치가 쌓일 수 있어 한 번에 다 내려주지 않는다.
    private static final int NOTIFICATION_PAGE_SIZE = 100;

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final UserNotificationSettingRepository userNotificationSettingRepository;
    private final UserRepository userRepository;

    // FR-NOTI-02. 같은 사용자·기기 조합이면 행을 새로 만들지 않고 토큰만 갱신한다.
    @Transactional
    public void registerDeviceToken(Long userId, RegisterDeviceTokenRequest request) {
        User user = findActiveUser(userId);

        // 같은 토큰이 다른 계정에 남아 있으면 내려둔다. 기기를 넘겨주거나 계정을 바꿔
        // 로그인했을 때 이전 사용자에게 알림이 가는 것을 막는다.
        deviceTokenRepository.findByToken(request.getToken())
                .filter(existing -> !existing.getUser().getUserId().equals(userId))
                .ifPresent(DeviceToken::deactivate);

        DeviceToken existing = findSameDeviceToken(userId, request);
        if (existing != null) {
            existing.refresh(request.getToken(), request.getPlatform());
            return;
        }

        deviceTokenRepository.save(new DeviceToken(
                user, request.getToken(), request.getPlatform(), request.getDeviceId()));
    }

    // 같은 기기의 기존 행을 찾는다. 기기 식별자를 보내면 그것으로 보고, 없으면 토큰으로 본다.
    // 기기 식별자 없이 토큰만 오는 경우 토큰이 갱신되면 같은 기기인지 알 수 없어 행이 하나 늘어난다.
    private DeviceToken findSameDeviceToken(Long userId, RegisterDeviceTokenRequest request) {
        if (request.getDeviceId() != null && !request.getDeviceId().isBlank()) {
            return deviceTokenRepository.findByUser_UserIdAndDeviceId(userId, request.getDeviceId())
                    .orElse(null);
        }
        return deviceTokenRepository.findByToken(request.getToken())
                .filter(token -> token.getUser().getUserId().equals(userId))
                .orElse(null);
    }

    // 로그아웃이나 기기 세션 해지 시 호출한다. 행은 지우지 않고 비활성으로 둔다.
    @Transactional
    public void unregisterDeviceToken(Long userId, String token) {
        deviceTokenRepository.findByToken(token)
                .filter(deviceToken -> deviceToken.getUser().getUserId().equals(userId))
                .ifPresent(DeviceToken::deactivate);
    }

    // FR-NOTI-03. 저장된 행이 없는 카테고리는 수신 허용으로 응답한다.
    public List<NotificationSettingResponse> getSettings(Long userId) {
        findActiveUser(userId);
        Map<NotificationCategory, Boolean> saved = userNotificationSettingRepository.findByUser_UserId(userId)
                .stream()
                .collect(Collectors.toMap(
                        UserNotificationSetting::getCategory, UserNotificationSetting::isEnabled));

        return Arrays.stream(NotificationCategory.values())
                .filter(NotificationCategory::isConfigurable)
                .map(category -> new NotificationSettingResponse(
                        category, saved.getOrDefault(category, true)))
                .toList();
    }

    @Transactional
    public NotificationSettingResponse updateSetting(Long userId, UpdateNotificationSettingRequest request) {
        User user = findActiveUser(userId);
        NotificationCategory category = request.getCategory();
        if (!category.isConfigurable()) {
            throw new CoreMateException(ErrorCode.NOTIFICATION_CATEGORY_NOT_CONFIGURABLE);
        }

        UserNotificationSetting setting = userNotificationSettingRepository
                .findByUser_UserIdAndCategory(userId, category)
                .orElseGet(() -> userNotificationSettingRepository.save(
                        new UserNotificationSetting(user, category, true)));
        setting.changeEnabled(request.getEnabled());

        return new NotificationSettingResponse(category, setting.isEnabled());
    }

    // FR-NOTI-05.
    public List<NotificationResponse> getNotifications(Long userId) {
        findActiveUser(userId);
        return notificationRepository
                .findByUser_UserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, NOTIFICATION_PAGE_SIZE))
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public long getUnreadCount(Long userId) {
        findActiveUser(userId);
        return notificationRepository.countByUser_UserIdAndReadFalse(userId);
    }

    // 본인 알림만 읽음 처리할 수 있다(AR-OWNER-06). 조회 조건에 userId 를 함께 걸어
    // 남의 알림 ID 를 넣으면 찾지 못하게 한다.
    @Transactional
    public NotificationResponse markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository
                .findByNotificationIdAndUser_UserId(notificationId, userId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.NOTIFICATION_NOT_FOUND));
        notification.markRead(LocalDateTime.now());
        return NotificationResponse.from(notification);
    }

    @Transactional
    public void markAllRead(Long userId) {
        findActiveUser(userId);
        notificationRepository.markAllRead(userId, LocalDateTime.now());
    }

    private User findActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new CoreMateException(ErrorCode.USER_NOT_FOUND));
    }
}
