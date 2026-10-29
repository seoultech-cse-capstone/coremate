package com.coremate.backend.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coremate.backend.auth.dto.request.OAuthLoginRequest;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Apple identity token 검증. 실제 Apple 키 대신 테스트에서 만든 RSA 키로 서명하고, 공개키 조회만 흉내 낸다.
 */
class AppleOAuthClientTest {

    private static final String CLIENT_ID = "com.coremate.app";
    private static final String ISSUER = "https://appleid.apple.com";
    private static final String KEY_ID = "test-kid";

    private final KeyPair appleKey = Jwts.SIG.RS256.keyPair().build();
    private final ApplePublicKeys publicKeys = mock(ApplePublicKeys.class);
    private final AppleTokenClient tokenClient = mock(AppleTokenClient.class);
    private final AppleOAuthClient client = new AppleOAuthClient(publicKeys, tokenClient, CLIENT_ID);

    @BeforeEach
    void setUp() {
        when(publicKeys.find(KEY_ID)).thenReturn(appleKey.getPublic());
    }

    @Test
    void 검증된_토큰이면_사용자_정보와_탈퇴용_refresh_token_을_돌려준다() {
        when(tokenClient.exchangeAuthorizationCode("auth-code")).thenReturn("apple-refresh");

        OAuthUserInfo info = client.fetch(request(token(ISSUER, CLIENT_ID, true, inMinutes(10)), " 홍길동 "));

        assertThat(info.providerId()).isEqualTo("apple-sub");
        assertThat(info.email()).isEqualTo("abc@privaterelay.appleid.com");
        assertThat(info.emailVerified()).isTrue();
        assertThat(info.name()).isEqualTo("홍길동");
        assertThat(info.providerRefreshToken()).isEqualTo("apple-refresh");
    }

    @Test
    void email_verified_가_문자열로_와도_검증된_이메일로_본다() {
        OAuthUserInfo info = client.fetch(request(token(ISSUER, CLIENT_ID, "true", inMinutes(10)), null));

        assertThat(info.emailVerified()).isTrue();
        assertThat(info.name()).isNull();
    }

    @Test
    void code_교환에_실패해도_로그인은_진행한다() {
        when(tokenClient.exchangeAuthorizationCode(any())).thenReturn(null);

        OAuthUserInfo info = client.fetch(request(token(ISSUER, CLIENT_ID, true, inMinutes(10)), null));

        assertThat(info.providerId()).isEqualTo("apple-sub");
        assertThat(info.providerRefreshToken()).isNull();
    }

    @Test
    void 다른_앱용으로_발급된_토큰은_거부한다() {
        assertInvalid(token(ISSUER, "com.other.app", true, inMinutes(10)));
    }

    @Test
    void Apple_이_발급하지_않은_토큰은_거부한다() {
        assertInvalid(token("https://evil.example.com", CLIENT_ID, true, inMinutes(10)));
    }

    @Test
    void 만료된_토큰은_거부한다() {
        assertInvalid(token(ISSUER, CLIENT_ID, true, inMinutes(-1)));
    }

    @Test
    void 다른_키로_서명된_토큰은_거부한다() {
        KeyPair otherKey = Jwts.SIG.RS256.keyPair().build();
        assertInvalid(signedToken(otherKey, KEY_ID, ISSUER, CLIENT_ID, true, inMinutes(10)));
    }

    @Test
    void 모르는_키로_서명된_토큰은_거부한다() {
        assertInvalid(signedToken(appleKey, "unknown-kid", ISSUER, CLIENT_ID, true, inMinutes(10)));
    }

    @Test
    void identity_token_이_없으면_검증하지_않고_거부한다() {
        assertInvalid(null);
        verify(publicKeys, never()).find(any());
        verify(tokenClient, never()).exchangeAuthorizationCode(any());
    }

    private void assertInvalid(String identityToken) {
        assertThatThrownBy(() -> client.fetch(request(identityToken, null)))
                .isInstanceOf(CoreMateException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_OAUTH_TOKEN);
    }

    private static OAuthLoginRequest request(String identityToken, String name) {
        return OAuthLoginRequest.builder()
                .idToken(identityToken)
                .authorizationCode("auth-code")
                .name(name)
                .build();
    }

    private String token(String issuer, String audience, Object emailVerified, Date expiresAt) {
        return signedToken(appleKey, KEY_ID, issuer, audience, emailVerified, expiresAt);
    }

    // Apple 처럼 aud 를 배열이 아닌 문자열로 넣는다.
    @SuppressWarnings("deprecation")
    private static String signedToken(KeyPair key, String keyId, String issuer, String audience,
                                      Object emailVerified, Date expiresAt) {
        return Jwts.builder()
                .header().keyId(keyId).and()
                .issuer(issuer)
                .audience().single(audience)
                .subject("apple-sub")
                .claim("email", "abc@privaterelay.appleid.com")
                .claim("email_verified", emailVerified)
                .issuedAt(Date.from(Instant.now().minus(Duration.ofMinutes(1))))
                .expiration(expiresAt)
                .signWith(key.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }

    private static Date inMinutes(long minutes) {
        return Date.from(Instant.now().plus(Duration.ofMinutes(minutes)));
    }
}
