package com.coremate.backend.notification.entity.type;

// 사용자가 수신 여부를 켜고 끄는 단위.
// SYSTEM 은 계정 보안 알림이라 설정 대상이 아니며 사용자가 끌 수 없다.
public enum NotificationCategory {
    DAILY,
    FRIEND,
    LEARNING,
    REWARD,
    SYSTEM;

    public boolean isConfigurable() {
        return this != SYSTEM;
    }
}
