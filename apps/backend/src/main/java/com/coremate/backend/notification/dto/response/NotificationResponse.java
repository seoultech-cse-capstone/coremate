package com.coremate.backend.notification.dto.response;

import com.coremate.backend.notification.entity.Notification;
import com.coremate.backend.notification.entity.type.NotificationCategory;
import com.coremate.backend.notification.entity.type.NotificationTargetType;
import com.coremate.backend.notification.entity.type.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "알림함 항목")
public record NotificationResponse(
        @Schema(example = "1") Long notificationId,
        @Schema(example = "FRIEND") NotificationCategory category,
        @Schema(example = "FRIEND_POKE") NotificationType notificationType,
        @Schema(example = "데모용주님이 콕 찔렀어요") String title,
        @Schema(example = "오늘의 퀴즈 풀러 갈까요?") String body,
        @Schema(example = "FRIEND") NotificationTargetType targetType,
        @Schema(example = "42") Long targetId,
        @Schema(example = "false") Boolean isRead,
        @Schema(example = "2026-08-06T19:10:11") LocalDateTime createdAt,
        @Schema(example = "2026-08-06T19:12:00") LocalDateTime readAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getCategory(),
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getTargetType(),
                notification.getTargetId(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}
