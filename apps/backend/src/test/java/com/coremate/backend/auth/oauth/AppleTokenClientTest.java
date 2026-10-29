package com.coremate.backend.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.coremate.backend.auth.event.UserWithdrawnEvent;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Apple 토큰 교환·해지 요청 모양과 client_secret 서명을 확인한다. Apple 서버 대신 MockRestServiceServer 를 쓴다.
 */
class AppleTokenClientTest {

    private static final String CLIENT_ID = "com.coremate.app";
    private static final String TEAM_ID = "TEAM123456";
    private static final String KEY_ID = "KEY1234567";
    private static final String TOKEN_URI = "https://appleid.apple.com/auth/token";
    private static final String REVOKE_URI = "https://appleid.apple.com/auth/revoke";

    private final KeyPair signInWithAppleKey = Jwts.SIG.ES256.keyPair().build();
    private final RestClient.Builder restClientBuilder = RestClient.builder();
    private final MockRestServiceServer appleServer = MockRestServiceServer.bindTo(restClientBuilder).build();

    @Test
    void client_secret_은_Apple_이_요구하는_헤더와_클레임으로_ES256_서명한다() {
        String secret = configuredClient().clientSecret();

        Jws<Claims> parsed = Jwts.parser()
                .verifyWith(signInWithAppleKey.getPublic())
                .build()
                .parseSignedClaims(secret);
        Claims claims = parsed.getPayload();
        assertThat(parsed.getHeader().getKeyId()).isEqualTo(KEY_ID);
        assertThat(parsed.getHeader().getAlgorithm()).isEqualTo("ES256");
        assertThat(claims.getIssuer()).isEqualTo(TEAM_ID);
        assertThat(claims.getSubject()).isEqualTo(CLIENT_ID);
        assertThat(Duration.between(claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant()))
                .isEqualTo(Duration.ofMinutes(5));
        // Apple 은 aud 를 배열로 보내면 거부하므로 문자열인지 원문으로 확인한다.
        String payload = new String(Base64.getUrlDecoder().decode(secret.split("\\.")[1]), StandardCharsets.UTF_8);
        assertThat(payload).contains("\"aud\":\"https://appleid.apple.com\"");
    }

    @Test
    void authorization_code_를_refresh_token_으로_바꾼다() {
        AppleTokenClient client = configuredClient();
        appleServer.expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formDataContains(Map.of(
                        "client_id", CLIENT_ID,
                        "code", "auth-code",
                        "grant_type", "authorization_code")))
                .andRespond(withSuccess("""
                        {"access_token":"a","token_type":"Bearer","expires_in":3600,
                         "refresh_token":"apple-refresh","id_token":"x"}""", MediaType.APPLICATION_JSON));

        assertThat(client.exchangeAuthorizationCode("auth-code")).isEqualTo("apple-refresh");
        appleServer.verify();
    }

    @Test
    void 교환에_실패하면_null_을_돌려준다() {
        AppleTokenClient client = configuredClient();
        appleServer.expect(requestTo(TOKEN_URI)).andRespond(withBadRequest());

        assertThat(client.exchangeAuthorizationCode("used-code")).isNull();
    }

    @Test
    void 탈퇴_이벤트의_refresh_token_을_전부_해지한다() {
        AppleTokenClient client = configuredClient();
        for (String token : List.of("refresh-1", "refresh-2")) {
            appleServer.expect(requestTo(REVOKE_URI))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().formDataContains(Map.of(
                            "client_id", CLIENT_ID,
                            "token", token,
                            "token_type_hint", "refresh_token")))
                    .andRespond(withSuccess());
        }

        client.revokeOnWithdraw(new UserWithdrawnEvent(1L, List.of("refresh-1", "refresh-2")));

        appleServer.verify();
    }

    @Test
    void 해지에_실패해도_예외를_던지지_않는다() {
        AppleTokenClient client = configuredClient();
        appleServer.expect(requestTo(REVOKE_URI)).andRespond(withServerError());

        client.revoke("refresh-1");

        appleServer.verify();
    }

    @Test
    void 키가_설정되지_않았으면_Apple_로_요청하지_않는다() {
        AppleTokenClient client = new AppleTokenClient(restClientBuilder, CLIENT_ID, "", "", "");

        assertThat(client.exchangeAuthorizationCode("auth-code")).isNull();
        client.revoke("refresh-1");

        appleServer.verify();
    }

    @Test
    void 키_내용이_잘못되면_기동할_때_알린다() {
        assertThatThrownBy(() -> new AppleTokenClient(restClientBuilder, CLIENT_ID, TEAM_ID, KEY_ID, "not-a-key"))
                .isInstanceOf(IllegalStateException.class);
    }

    private AppleTokenClient configuredClient() {
        return new AppleTokenClient(restClientBuilder, CLIENT_ID, TEAM_ID, KEY_ID, pem());
    }

    // 개발자 사이트에서 받는 .p8 파일과 같은 PKCS#8 PEM.
    private String pem() {
        String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(signInWithAppleKey.getPrivate().getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + body + "\n-----END PRIVATE KEY-----\n";
    }
}
