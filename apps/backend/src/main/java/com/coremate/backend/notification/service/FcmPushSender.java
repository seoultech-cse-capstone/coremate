package com.coremate.backend.notification.service;

import com.coremate.backend.notification.entity.type.SendFailReason;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// FCM HTTP v1 으로 실제 푸시를 보낸다.
@Slf4j
@RequiredArgsConstructor
public class FcmPushSender implements PushSender {

    // 앱이 만들어 둔 채널. 지정하지 않으면 FCM 이 fcm_fallback_notification_channel 을
    // 따로 만들어 쓰기 때문에, 앱에서 채널별 설정을 해도 서버 발송분에는 적용되지 않는다.
    private static final String ANDROID_CHANNEL_ID = "default";

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public PushSendResult send(String token, String title, String body, Map<String, String> data) {
        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .setAndroidConfig(AndroidConfig.builder()
                        .setNotification(AndroidNotification.builder()
                                .setChannelId(ANDROID_CHANNEL_ID)
                                .build())
                        .build())
                .putAllData(data)
                .build();

        try {
            firebaseMessaging.send(message);
            return PushSendResult.success();
        } catch (FirebaseMessagingException e) {
            SendFailReason reason = resolveFailReason(e.getMessagingErrorCode());
            log.warn("FCM 발송 실패. errorCode={} reason={}", e.getMessagingErrorCode(), reason);
            return PushSendResult.failure(reason);
        }
    }

    // 토큰을 비활성화해도 되는 응답만 TOKEN_EXPIRED 로 분류한다.
    //
    // INVALID_ARGUMENT 는 토큰이 잘못됐을 때도 나오지만 메시지 본문이 규격에 안 맞을 때도
    // 나온다. 이걸 토큰 문제로 처리하면 멀쩡한 기기의 토큰을 내려 버려 그 사용자는 이후
    // 알림을 영영 못 받는다. 원인이 분명한 두 가지만 토큰 문제로 본다.
    private SendFailReason resolveFailReason(MessagingErrorCode errorCode) {
        if (errorCode == MessagingErrorCode.UNREGISTERED
                || errorCode == MessagingErrorCode.SENDER_ID_MISMATCH) {
            return SendFailReason.TOKEN_EXPIRED;
        }
        return SendFailReason.FCM_ERROR;
    }
}
