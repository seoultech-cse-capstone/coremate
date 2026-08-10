package com.coremate.backend.notification.entity.type;

// 발송이 성공하지 못한 사유.
public enum SendFailReason {
    NO_TOKEN,
    TOKEN_EXPIRED,
    FCM_ERROR,
    OPTED_OUT,
    PERMISSION_DENIED
}
