package com.coremate.backend.notification.service;

import com.coremate.backend.notification.entity.type.DevicePlatform;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

// 자격증명(Firebase 또는 APNs 키)이 없는 환경에서 쓰는 구현. 발송 대상과 내용을 로그로만 남기고
// 성공으로 처리해, 자격증명 없이도 발송 판단 로직을 그대로 검증할 수 있게 한다.
@Slf4j
public class LoggingPushSender implements PushSender {

    @Override
    public PushSendResult send(
            DevicePlatform platform, String token, String title, String body, Map<String, String> data) {
        log.info("[PUSH] platform={} token={} title={} body={} data={}",
                platform, maskToken(token), title, body, data);
        return PushSendResult.success();
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 12) {
            return "****";
        }
        return token.substring(0, 8) + "..." + token.substring(token.length() - 4);
    }
}
