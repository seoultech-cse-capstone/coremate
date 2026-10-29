package com.coremate.backend.auth.oauth;

import com.coremate.backend.auth.dto.request.OAuthLoginRequest;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.user.entity.SocialProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 카카오 access token 으로 사용자 정보를 조회한다.
 * 별도 서버 시크릿 없이 토큰만으로 조회하므로 provider 콘솔의 이메일 동의항목 설정에 의존한다.
 */
@Component
public class KakaoOAuthClient implements OAuthClient {

    private static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient = RestClient.create();

    @Override
    public SocialProvider provider() {
        return SocialProvider.KAKAO;
    }

    @Override
    public OAuthUserInfo fetch(OAuthLoginRequest request) {
        String accessToken = request.getAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            throw new CoreMateException(ErrorCode.INVALID_OAUTH_TOKEN);
        }

        KakaoUserResponse response;
        try {
            response = restClient.get()
                    .uri(USER_INFO_URI)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(KakaoUserResponse.class);
        } catch (RestClientException e) {
            // 토큰 무효·만료·조회 실패는 전부 인증 실패로 본다.
            throw new CoreMateException(ErrorCode.INVALID_OAUTH_TOKEN);
        }

        if (response == null || response.id() == null) {
            throw new CoreMateException(ErrorCode.INVALID_OAUTH_TOKEN);
        }

        KakaoAccount account = response.kakao_account();
        String email = account != null ? account.email() : null;
        boolean emailVerified = account != null
                && Boolean.TRUE.equals(account.is_email_valid())
                && Boolean.TRUE.equals(account.is_email_verified());
        // 비즈앱 실명(name) 동의항목을 우선 사용하고, 없으면 닉네임으로 대체한다.
        String name = null;
        if (account != null) {
            if (account.name() != null) {
                name = account.name();
            } else if (account.profile() != null) {
                name = account.profile().nickname();
            }
        }

        return new OAuthUserInfo(String.valueOf(response.id()), email, emailVerified, name);
    }

    // 카카오 /v2/user/me 응답에서 필요한 필드만. 필드명을 카카오 JSON 키와 일치시켜
    // 별도 매핑 설정 없이 그대로 바인딩한다.
    private record KakaoUserResponse(Long id, KakaoAccount kakao_account) {
    }

    private record KakaoAccount(String name, String email, Boolean is_email_valid, Boolean is_email_verified,
                                KakaoProfile profile) {
    }

    private record KakaoProfile(String nickname) {
    }
}
