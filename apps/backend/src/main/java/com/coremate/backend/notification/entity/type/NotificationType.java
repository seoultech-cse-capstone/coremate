package com.coremate.backend.notification.entity.type;

// 알림 유형 9종. 각 유형은 하나의 카테고리에 속하고, 수신 여부는 카테고리 단위로 판단한다.
public enum NotificationType {
    DAILY_QUIZ_OPEN(NotificationCategory.DAILY),
    ATTENDANCE_REMINDER(NotificationCategory.DAILY),
    FRIEND_POKE(NotificationCategory.FRIEND),
    FRIEND_REQUEST_RECEIVED(NotificationCategory.FRIEND),
    FRIEND_REQUEST_ACCEPTED(NotificationCategory.FRIEND),
    LEARNING_REMINDER(NotificationCategory.LEARNING),
    LEVEL_UP(NotificationCategory.REWARD),
    NOTICE_EVENT(NotificationCategory.REWARD),
    NEW_DEVICE_LOGIN(NotificationCategory.SYSTEM);

    private final NotificationCategory category;

    NotificationType(NotificationCategory category) {
        this.category = category;
    }

    public NotificationCategory getCategory() {
        return category;
    }
}
