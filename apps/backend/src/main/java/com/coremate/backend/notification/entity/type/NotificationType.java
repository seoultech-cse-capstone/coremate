package com.coremate.backend.notification.entity.type;

// 알림 유형 8종. 각 유형은 하나의 카테고리에 속하고, 수신 여부는 카테고리 단위로 판단한다.
// 유형을 추가하려면 NotificationMessages 에 문구 팩토리와 실제 발행처를 함께 만들 것.
// 보낼 방법 없는 유형만 남으면 문서를 읽은 사람이 있는 기능으로 오해한다.
public enum NotificationType {
    DAILY_QUIZ_OPEN(NotificationCategory.DAILY),
    ATTENDANCE_REMINDER(NotificationCategory.DAILY),
    FRIEND_POKE(NotificationCategory.FRIEND),
    FRIEND_REQUEST_RECEIVED(NotificationCategory.FRIEND),
    FRIEND_REQUEST_ACCEPTED(NotificationCategory.FRIEND),
    LEARNING_REMINDER(NotificationCategory.LEARNING),
    LEVEL_UP(NotificationCategory.REWARD),
    NEW_DEVICE_LOGIN(NotificationCategory.SYSTEM);

    private final NotificationCategory category;

    NotificationType(NotificationCategory category) {
        this.category = category;
    }

    public NotificationCategory getCategory() {
        return category;
    }
}
