package com.coremate.backend.notification.service;

import java.util.Map;

// 실제 푸시 전송 수단. 지금은 로그만 남기는 구현이 붙어 있고, FCM 연동 시 구현체만 바뀐다.
// 인터페이스로 분리해 두면 FCM 자격증명 없이도 발송 파이프라인 전체를 검증할 수 있다.
public interface PushSender {

    PushSendResult send(String token, String title, String body, Map<String, String> data);
}
