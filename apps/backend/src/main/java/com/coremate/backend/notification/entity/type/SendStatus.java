package com.coremate.backend.notification.entity.type;

// 발송 시도 결과. SKIPPED 는 수신 거부나 토큰 없음으로 시도조차 하지 않은 경우다.
// 알림이 오지 않았다는 문의를 추적하려면 이 경우도 남겨야 한다.
public enum SendStatus {
    SUCCESS,
    FAILED,
    SKIPPED
}
