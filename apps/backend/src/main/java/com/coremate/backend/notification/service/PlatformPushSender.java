package com.coremate.backend.notification.service;

import com.coremate.backend.notification.entity.type.DevicePlatform;
import java.util.Map;
import lombok.RequiredArgsConstructor;

// 기기 플랫폼에 맞는 전송기로 넘긴다.
//
// 앱은 두 플랫폼 모두 getDevicePushTokenAsync 로 받은 네이티브 토큰을 등록한다. 안드로이드는 FCM 토큰이지만
// 아이폰은 APNs 토큰이라 FCM 으로는 보낼 수 없다(FCM 이 형식 오류로 거절한다).
@RequiredArgsConstructor
public class PlatformPushSender implements PushSender {

    private final PushSender android;
    private final PushSender ios;

    @Override
    public PushSendResult send(
            DevicePlatform platform, String token, String title, String body, Map<String, String> data) {
        PushSender target = platform == DevicePlatform.IOS ? ios : android;
        return target.send(platform, token, title, body, data);
    }
}
