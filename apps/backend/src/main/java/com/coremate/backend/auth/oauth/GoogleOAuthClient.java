package com.coremate.backend.auth.oauth;

import com.coremate.backend.auth.dto.request.OAuthLoginRequest;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.user.entity.SocialProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 구글 idToken 을 검증해 사용자 정보를 조회한다.
 * 카카오/네이버와 달리 앱이 준 idToken(JWT)을 구글 tokeninfo 로 검증하고, aud 가 우리 웹 클라이언트 ID 와
 * 일치하는지 확인해 다른 앱용으로 발급된 토큰의 재사용을 막는다. client-id 는 공개값이라 서버 시크릿은 아니다.
 */
@Component
public class GoogleOAuthClient implements OAuthClient {

    private static final String TOKEN_INFO_URI = "https://oauth2.googleapis.com/tokeninfo";

    private final RestClient restClient = RestClient.create();
    private final String clientId;

    public GoogleOAuthClient(@Value("${oauth.google.client-id:}") String clientId) {
        this.clientId = clientId;
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.GOOGLE;
    }

    @Override
    public OAuthUserInfo fetch(OAuthLoginRequest request) {
        String idToken = request.getIdToken();
        if (idToken == null || idToken.isBlank()) {
            throw new CoreMateException(ErrorCode.INVALID_OAUTH_TOKEN);
        }

        GoogleTokenInfo info;
        try {
            String uri = UriComponentsBuilder.fromUriString(TOKEN_INFO_URI)
                    .queryParam("id_token", idToken)
                    .toUriString();
            info = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(GoogleTokenInfo.class);
        } catch (RestClientException e) {
            // 만료·위조·조회 실패는 전부 인증 실패로 본다.
            throw new CoreMateException(ErrorCode.INVALID_OAUTH_TOKEN);
        }

        if (info == null || info.sub() == null) {
            throw new CoreMateException(ErrorCode.INVALID_OAUTH_TOKEN);
        }
        // aud 가 우리 앱(웹 클라이언트 ID)용으로 발급된 토큰인지 확인한다. 설정이 비었거나 불일치면 거부.
        if (clientId.isBlank() || !clientId.equals(info.aud())) {
            throw new CoreMateException(ErrorCode.INVALID_OAUTH_TOKEN);
        }

        boolean emailVerified = "true".equalsIgnoreCase(info.email_verified());
        return new OAuthUserInfo(info.sub(), info.email(), emailVerified, info.name());
    }

    // 구글 tokeninfo 응답에서 필요한 필드만. email_verified 는 문자열("true"/"false") 로 온다.
    private record GoogleTokenInfo(String aud, String sub, String email, String email_verified, String name) {
    }
}
