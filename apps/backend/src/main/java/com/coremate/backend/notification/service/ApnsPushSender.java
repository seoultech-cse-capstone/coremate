package com.coremate.backend.notification.service;

import com.coremate.backend.notification.entity.type.DevicePlatform;
import com.coremate.backend.notification.entity.type.SendFailReason;
import io.jsonwebtoken.Jwts;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// 아이폰 알림을 APNs(애플 푸시 서버)에 HTTP/2 로 직접 보낸다.
//
// 인증은 개발자 사이트에서 만든 .p8 키로 서명한 JWT 를 쓴다. 인증서 방식과 달리 해마다 갱신할 필요가 없다.
@Slf4j
public class ApnsPushSender implements PushSender {

    private static final String PRODUCTION_HOST = "https://api.push.apple.com";
    private static final String SANDBOX_HOST = "https://api.sandbox.push.apple.com";

    // APNs 는 인증 JWT 를 1시간 안에 새로 만들되 20분보다 자주 바꾸지 말라고 한다(어기면 거절한다).
    private static final Duration AUTH_TOKEN_TTL = Duration.ofMinutes(50);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    // APNs 기기 토큰은 16진수 문자열이다. 아니면 보내 봐야 거절되므로 요청하지 않는다.
    private static final Pattern DEVICE_TOKEN = Pattern.compile("[0-9a-fA-F]+");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String host;
    private final String topic;
    private final String teamId;
    private final String keyId;
    private final PrivateKey privateKey;

    private String authToken;
    private Instant authTokenIssuedAt;

    public ApnsPushSender(ObjectMapper objectMapper, String teamId, String keyId, String privateKeyPem,
                          String topic, boolean production) {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = objectMapper;
        this.host = production ? PRODUCTION_HOST : SANDBOX_HOST;
        this.topic = topic;
        this.teamId = teamId;
        this.keyId = keyId;
        this.privateKey = parsePrivateKey(privateKeyPem);
    }

    @Override
    public PushSendResult send(
            DevicePlatform platform, String token, String title, String body, Map<String, String> data) {
        if (token == null || !DEVICE_TOKEN.matcher(token).matches()) {
            return PushSendResult.failure(SendFailReason.TOKEN_EXPIRED);
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(host + "/3/device/" + token))
                .timeout(REQUEST_TIMEOUT)
                .header("authorization", "bearer " + authToken())
                .header("apns-topic", topic)
                .header("apns-push-type", "alert")
                .header("apns-priority", "10")
                .POST(HttpRequest.BodyPublishers.ofString(payload(title, body, data)))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return PushSendResult.success();
            }
            if (response.statusCode() == 403) {
                // 인증 JWT 문제(만료·서명 오류). 다음 발송 때 새로 만든다.
                resetAuthToken();
            }
            String reason = reasonOf(response.body());
            SendFailReason failReason = resolveFailReason(response.statusCode(), reason);
            log.warn("APNs 발송 실패. status={} reason={} failReason={}", response.statusCode(), reason, failReason);
            return PushSendResult.failure(failReason);
        } catch (IOException e) {
            log.warn("APNs 연결 실패: {}", e.getMessage());
            return PushSendResult.failure(SendFailReason.APNS_ERROR);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return PushSendResult.failure(SendFailReason.APNS_ERROR);
        }
    }

    // expo-notifications 는 원격 알림의 data 를 userInfo["body"] 에서 꺼낸다(NotificationRecords.swift).
    // 알림을 눌렀을 때 화면 이동에 쓰는 값은 그래서 aps 밖의 "body" 에 담는다.
    String payload(String title, String body, Map<String, String> data) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("aps", Map.of(
                "alert", Map.of("title", title, "body", body),
                "sound", "default"));
        root.put("body", data);
        return objectMapper.writeValueAsString(root);
    }

    // 토큰을 비활성화해도 되는 응답만 TOKEN_EXPIRED 로 분류한다(FcmPushSender 와 같은 기준).
    //
    // 410 Unregistered: 앱을 지웠거나 알림을 끈 기기. BadDeviceToken: 토큰 형식이 틀렸거나 환경이 다르다
    // (Xcode 로 직접 설치한 개발 빌드의 sandbox 토큰이 운영 서버로 온 경우). DeviceTokenNotForTopic: 다른 앱의 토큰.
    // 셋 다 같은 토큰으로 다시 보내도 영영 실패한다. 앱은 켤 때마다 토큰을 다시 등록하므로 잘못 내려도 되살아난다.
    static SendFailReason resolveFailReason(int status, String reason) {
        if (status == 410
                || "BadDeviceToken".equals(reason)
                || "DeviceTokenNotForTopic".equals(reason)) {
            return SendFailReason.TOKEN_EXPIRED;
        }
        return SendFailReason.APNS_ERROR;
    }

    private String reasonOf(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            JsonNode reason = objectMapper.readTree(responseBody).get("reason");
            return reason == null || reason.isNull() ? null : reason.asString();
        } catch (JacksonException e) {
            return null;
        }
    }

    private synchronized String authToken() {
        Instant now = Instant.now();
        if (authToken == null || authTokenIssuedAt.plus(AUTH_TOKEN_TTL).isBefore(now)) {
            authToken = Jwts.builder()
                    .header().keyId(keyId).and()
                    .issuer(teamId)
                    .issuedAt(Date.from(now))
                    .signWith(privateKey, Jwts.SIG.ES256)
                    .compact();
            authTokenIssuedAt = now;
        }
        return authToken;
    }

    private synchronized void resetAuthToken() {
        authToken = null;
    }

    // .p8 파일 내용(PEM) 그대로 받는다. 환경변수에 한 줄로 넣느라 줄바꿈을 \n 글자로 쓴 경우도 읽는다.
    private static PrivateKey parsePrivateKey(String pem) {
        String base64 = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\n", "")
                .replaceAll("\\s", "");
        try {
            byte[] der = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            throw new IllegalStateException("apns.private-key 를 읽지 못했다. .p8 파일 내용 전체를 넣어야 한다.", e);
        }
    }
}
