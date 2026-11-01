package com.coremate.backend.notification.service;

import com.coremate.backend.notification.entity.type.DevicePlatform;
import java.util.Map;

// 실제 푸시 전송 수단. 안드로이드는 FCM, 아이폰은 APNs 로 보내고, 자격증명이 없으면 로그만 남긴다.
// 인터페이스로 분리해 두면 자격증명 없이도 발송 파이프라인 전체를 검증할 수 있다.
public interface PushSender {

    PushSendResult send(DevicePlatform platform, String token, String title, String body, Map<String, String> data);
}
