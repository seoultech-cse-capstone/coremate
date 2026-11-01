package com.coremate.backend.notification.entity.type;

// 발송이 성공하지 못한 사유.
public enum SendFailReason {
    NO_TOKEN,
    TOKEN_EXPIRED,
    FCM_ERROR,
    OPTED_OUT,
    PERMISSION_DENIED,
    // 아이폰(APNs) 발송 실패. 토큰 문제는 위 TOKEN_EXPIRED 로 따로 분류한다.
    APNS_ERROR
}
