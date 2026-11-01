package com.coremate.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.coremate.backend.notification.entity.type.DevicePlatform;
import com.coremate.backend.notification.entity.type.SendFailReason;
import java.util.Map;
import org.junit.jupiter.api.Test;

// 아이폰 토큰을 FCM 으로 보내면 형식 오류로 거절된다. 플랫폼별로 맞는 전송기로 가는지만 본다.
class PlatformPushSenderTest {

    private final PushSender android = mock(PushSender.class);
    private final PushSender ios = mock(PushSender.class);
    private final PlatformPushSender sender = new PlatformPushSender(android, ios);

    @Test
    void iosTokenGoesToIosSender() {
        given(ios.send(any(), any(), any(), any(), any())).willReturn(PushSendResult.success());

        PushSendResult result = sender.send(DevicePlatform.IOS, "ab12", "제목", "내용", Map.of());

        assertThat(result.successful()).isTrue();
        verifyNoInteractions(android);
    }

    @Test
    void androidTokenGoesToAndroidSender() {
        given(android.send(any(), any(), any(), any(), any()))
                .willReturn(PushSendResult.failure(SendFailReason.FCM_ERROR));

        PushSendResult result = sender.send(DevicePlatform.ANDROID, "fcm:token", "제목", "내용", Map.of());

        assertThat(result.failReason()).isEqualTo(SendFailReason.FCM_ERROR);
        verifyNoInteractions(ios);
    }
}
