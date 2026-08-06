package com.coremate.backend.notification.service;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// 실제 FCM 발송 전까지 쓰는 구현. 발송 대상과 내용을 로그로만 남기고 성공으로 처리한다.
// 이 단계에서 검증하려는 것은 "누구에게 무엇을 보내기로 판단했는가"이지 전송 자체가 아니다.
@Slf4j
@Component
public class LoggingPushSender implements PushSender {

    @Override
    public PushSendResult send(String token, String title, String body, Map<String, String> data) {
        log.info("[PUSH] token={} title={} body={} data={}", maskToken(token), title, body, data);
        return PushSendResult.success();
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 12) {
            return "****";
        }
        return token.substring(0, 8) + "..." + token.substring(token.length() - 4);
    }
}
