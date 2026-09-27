package com.coremate.backend.notification.entity.type;

// 알림을 눌렀을 때 이동할 화면 종류. 종류마다 target_id 가 가리키는 대상이 달라
// 물리 외래키를 두지 않고 앱이 해석한다.
public enum NotificationTargetType {
    NONE,
    FRIEND,
    DAILY_QUIZ,
    MYPAGE,
    SETTING
}
