package com.coremate.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coremate.backend.notification.entity.type.DevicePlatform;
import com.coremate.backend.notification.entity.type.SendFailReason;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

// 실제 APNs 는 부르지 않는다. 앱이 읽는 본문 모양과, 응답별로 토큰을 내릴지 판단하는 기준만 본다.
class ApnsPushSenderTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void payloadPutsDataUnderBodyWhereExpoNotificationsReadsIt() {
        ApnsPushSender sender = newSender();

        JsonNode payload = objectMapper.readTree(sender.payload("콕!", "친구가 콕 찔렀어요",
                Map.of("notificationId", "7", "targetType", "FRIEND")));

        assertThat(payload.at("/aps/alert/title").asString()).isEqualTo("콕!");
        assertThat(payload.at("/aps/alert/body").asString()).isEqualTo("친구가 콕 찔렀어요");
        assertThat(payload.at("/aps/sound").asString()).isEqualTo("default");
        // 알림을 눌렀을 때 화면 이동에 쓰는 값. aps 안이나 최상위에 두면 앱이 못 읽는다.
        assertThat(payload.at("/body/targetType").asString()).isEqualTo("FRIEND");
        assertThat(payload.at("/body/notificationId").asString()).isEqualTo("7");
    }

    @Test
    void onlyResponsesThatMeanTheTokenIsDeadDeactivateIt() {
        assertThat(ApnsPushSender.resolveFailReason(410, "Unregistered")).isEqualTo(SendFailReason.TOKEN_EXPIRED);
        assertThat(ApnsPushSender.resolveFailReason(400, "BadDeviceToken")).isEqualTo(SendFailReason.TOKEN_EXPIRED);
        assertThat(ApnsPushSender.resolveFailReason(400, "DeviceTokenNotForTopic"))
                .isEqualTo(SendFailReason.TOKEN_EXPIRED);

        // 본문·인증·서버 문제로 멀쩡한 토큰을 내리면 그 사용자는 앱을 다시 켤 때까지 알림을 못 받는다.
        assertThat(ApnsPushSender.resolveFailReason(400, "PayloadTooLarge")).isEqualTo(SendFailReason.APNS_ERROR);
        assertThat(ApnsPushSender.resolveFailReason(403, "InvalidProviderToken")).isEqualTo(SendFailReason.APNS_ERROR);
        assertThat(ApnsPushSender.resolveFailReason(429, "TooManyRequests")).isEqualTo(SendFailReason.APNS_ERROR);
        assertThat(ApnsPushSender.resolveFailReason(503, null)).isEqualTo(SendFailReason.APNS_ERROR);
    }

    @Test
    void tokenThatIsNotHexIsTreatedAsDeadWithoutCallingApns() {
        ApnsPushSender sender = newSender();

        PushSendResult result = sender.send(DevicePlatform.IOS, "fcm:not-an-apns-token", "제목", "내용", Map.of());

        assertThat(result.tokenInvalid()).isTrue();
    }

    @Test
    void brokenKeyFailsAtStartupInsteadOfAtFirstPush() {
        assertThatThrownBy(() -> new ApnsPushSender(objectMapper, "TEAMID", "KEYID", "not-a-key",
                "com.coremate.app", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("apns.private-key");
    }

    private ApnsPushSender newSender() {
        return new ApnsPushSender(objectMapper, "TEAMID1234", "KEYID12345", generatedP8(), "com.coremate.app", true);
    }

    // 개발자 사이트에서 받는 .p8 과 같은 모양(PKCS#8, P-256)의 키를 테스트마다 만든다.
    private static String generatedP8() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            String base64 = Base64.getMimeEncoder(64, "\n".getBytes())
                    .encodeToString(generator.generateKeyPair().getPrivate().getEncoded());
            return "-----BEGIN PRIVATE KEY-----\n" + base64 + "\n-----END PRIVATE KEY-----";
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
