package com.coremate.backend.auth.oauth;

import com.coremate.backend.auth.event.UserWithdrawnEvent;
import io.jsonwebtoken.Jwts;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Apple 로그인 토큰 교환·해지(Sign in with Apple REST API).
 *
 * <p>Apple 은 Apple 로그인을 넣은 앱에서 사용자가 탈퇴하면 그 사용자의 토큰을 해지해 앱과의 연결을 끊으라고 한다.
 * 해지에는 refresh token 이 필요해서, 로그인할 때 앱이 준 authorization code 를 refresh token 으로 바꿔 둔다.
 * 두 요청 모두 Sign in with Apple 키(.p8)로 서명한 client_secret 이 필요하다. 키가 설정되지 않은 환경에서는
 * 아무것도 하지 않는다(로그인은 되고 탈퇴할 때 연결 끊기만 빠진다).
 */
@Component
@Slf4j
public class AppleTokenClient {

    private static final String TOKEN_URI = "https://appleid.apple.com/auth/token";
    private static final String REVOKE_URI = "https://appleid.apple.com/auth/revoke";
    private static final String AUDIENCE = "https://appleid.apple.com";
    // client_secret 은 요청할 때마다 새로 만들어 짧게 둔다(Apple 은 최대 6개월까지 허용).
    private static final Duration CLIENT_SECRET_TTL = Duration.ofMinutes(5);

    private final RestClient restClient;
    private final String clientId;
    private final String teamId;
    private final String keyId;
    private final PrivateKey privateKey;

    @Autowired
    public AppleTokenClient(@Value("${oauth.apple.client-id:}") String clientId,
                            @Value("${oauth.apple.team-id:}") String teamId,
                            @Value("${oauth.apple.key-id:}") String keyId,
                            @Value("${oauth.apple.private-key:}") String privateKeyPem) {
        this(RestClient.builder(), clientId, teamId, keyId, privateKeyPem);
    }

    AppleTokenClient(RestClient.Builder restClientBuilder, String clientId, String teamId, String keyId,
                     String privateKeyPem) {
        this.restClient = restClientBuilder.build();
        this.clientId = clientId;
        this.teamId = teamId;
        this.keyId = keyId;

        boolean configured = Stream.of(clientId, teamId, keyId, privateKeyPem).allMatch(StringUtils::hasText);
        if (!configured && Stream.of(teamId, keyId, privateKeyPem).anyMatch(StringUtils::hasText)) {
            log.warn("oauth.apple 키 설정이 일부만 있어 탈퇴할 때 Apple 연결 끊기를 건너뛴다.");
        }
        this.privateKey = configured ? parsePrivateKey(privateKeyPem) : null;
    }

    /** authorization code 를 refresh token 으로 바꾼다. 키가 없거나 실패하면 null(로그인은 막지 않는다). */
    public String exchangeAuthorizationCode(String authorizationCode) {
        if (privateKey == null || authorizationCode == null || authorizationCode.isBlank()) {
            return null;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret());
        form.add("code", authorizationCode);
        form.add("grant_type", "authorization_code");
        try {
            AppleTokenResponse response = restClient.post()
                    .uri(TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(AppleTokenResponse.class);
            return response == null ? null : response.refresh_token();
        } catch (RestClientException e) {
            log.warn("Apple authorization code 교환 실패 — 이 사용자는 탈퇴할 때 Apple 연결을 끊지 못한다: {}",
                    e.getMessage());
            return null;
        }
    }

    /** refresh token 을 해지해 Apple 과의 연결을 끊는다. 탈퇴는 이미 끝났으므로 실패해도 로그만 남긴다. */
    public void revoke(String refreshToken) {
        if (privateKey == null || refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret());
        form.add("token", refreshToken);
        form.add("token_type_hint", "refresh_token");
        try {
            restClient.post()
                    .uri(REVOKE_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Apple 토큰 해지 실패: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void revokeOnWithdraw(UserWithdrawnEvent event) {
        event.appleRefreshTokens().forEach(this::revoke);
    }

    // audience().single() 은 jjwt 가 "배열 aud 를 못 받는 시스템용"으로만 남겨 deprecated 로 표시한 것이다.
    @SuppressWarnings("deprecation")
    String clientSecret() {
        Instant now = Instant.now();
        return Jwts.builder()
                .header().keyId(keyId).and()
                .issuer(teamId)
                .subject(clientId)
                // Apple 은 aud 를 배열이 아닌 문자열로 받는다.
                .audience().single(AUDIENCE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(CLIENT_SECRET_TTL)))
                .signWith(privateKey, Jwts.SIG.ES256)
                .compact();
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
            throw new IllegalStateException("oauth.apple.private-key 를 읽지 못했다. .p8 파일 내용 전체를 넣어야 한다.", e);
        }
    }

    // Apple 토큰 응답에서 필요한 필드만.
    private record AppleTokenResponse(String refresh_token) {
    }
}
