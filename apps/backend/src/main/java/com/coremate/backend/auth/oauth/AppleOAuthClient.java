package com.coremate.backend.auth.oauth;

import com.coremate.backend.auth.dto.request.OAuthLoginRequest;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.user.entity.SocialProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.ProtectedHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Apple 로그인. 앱이 준 identity token(JWT)을 Apple 공개키로 검증하고 발급자·수신자(우리 번들 ID)·만료를 확인한다.
 *
 * <p>Apple 은 이름을 토큰에 넣지 않고 처음 로그인할 때만 앱에 주므로, 이름은 앱이 요청에 담아 보낸 값을 쓴다.
 * 함께 받은 authorization code 는 refresh token 으로 바꿔 두었다가 탈퇴할 때 Apple 연결을 끊는 데 쓴다
 * ({@link AppleTokenClient}). 바꾸지 못해도 로그인은 그대로 진행한다.
 */
@Component
public class AppleOAuthClient implements OAuthClient {

    private static final String ISSUER = "https://appleid.apple.com";

    private final ApplePublicKeys publicKeys;
    private final AppleTokenClient tokenClient;
    private final String clientId;

    public AppleOAuthClient(ApplePublicKeys publicKeys,
                            AppleTokenClient tokenClient,
                            @Value("${oauth.apple.client-id:}") String clientId) {
        this.publicKeys = publicKeys;
        this.tokenClient = tokenClient;
        this.clientId = clientId;
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.APPLE;
    }

    @Override
    public OAuthUserInfo fetch(OAuthLoginRequest request) {
        String identityToken = request.getIdToken();
        if (identityToken == null || identityToken.isBlank() || clientId.isBlank()) {
            throw new CoreMateException(ErrorCode.INVALID_OAUTH_TOKEN);
        }

        Claims claims;
        try {
            claims = Jwts.parser()
                    .keyLocator(header -> header instanceof ProtectedHeader protectedHeader
                            ? publicKeys.find(protectedHeader.getKeyId())
                            : null)
                    .requireIssuer(ISSUER)
                    // 다른 앱용으로 발급된 토큰의 재사용을 막는다.
                    .requireAudience(clientId)
                    .build()
                    .parseSignedClaims(identityToken)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            // 서명·발급자·수신자 불일치, 만료, 모르는 키는 전부 인증 실패로 본다.
            throw new CoreMateException(ErrorCode.INVALID_OAUTH_TOKEN);
        }

        if (claims.getSubject() == null) {
            throw new CoreMateException(ErrorCode.INVALID_OAUTH_TOKEN);
        }

        // email_verified 는 문자열("true")로 올 때도, 불리언으로 올 때도 있다.
        Object emailVerified = claims.get("email_verified");
        boolean verified = Boolean.TRUE.equals(emailVerified) || "true".equals(emailVerified);
        String refreshToken = tokenClient.exchangeAuthorizationCode(request.getAuthorizationCode());

        return new OAuthUserInfo(claims.getSubject(), claims.get("email", String.class), verified,
                blankToNull(request.getName()), refreshToken);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
