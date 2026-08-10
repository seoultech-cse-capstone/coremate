package com.coremate.backend.notification.service;

import com.coremate.backend.notification.entity.type.NotificationTargetType;
import com.coremate.backend.notification.entity.type.NotificationType;

// 알림 발송 요청. 어떤 사용자에게 어떤 유형의 알림을 무슨 내용으로 보낼지와,
// 알림을 눌렀을 때 이동할 대상을 담는다.
public record NotificationCommand(
        Long userId,
        NotificationType type,
        String title,
        String body,
        NotificationTargetType targetType,
        Long targetId
) {

    public static NotificationCommand of(Long userId, NotificationType type, String title, String body) {
        return new NotificationCommand(userId, type, title, body, NotificationTargetType.NONE, null);
    }

    public static NotificationCommand of(
            Long userId,
            NotificationType type,
            String title,
            String body,
            NotificationTargetType targetType,
            Long targetId
    ) {
        return new NotificationCommand(userId, type, title, body, targetType, targetId);
    }
}
