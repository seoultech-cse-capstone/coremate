package com.coremate.backend.notification.service;

import com.coremate.backend.notification.entity.type.SendFailReason;

// 푸시 발송 시도 결과. 실패 사유는 발송 로그에 그대로 남는다.
public record PushSendResult(boolean successful, SendFailReason failReason) {

    public static PushSendResult success() {
        return new PushSendResult(true, null);
    }

    public static PushSendResult failure(SendFailReason failReason) {
        return new PushSendResult(false, failReason);
    }

    // 토큰이 더 이상 유효하지 않다는 응답. 해당 기기 토큰을 비활성화해야 한다.
    public boolean tokenInvalid() {
        return failReason == SendFailReason.TOKEN_EXPIRED;
    }
}
