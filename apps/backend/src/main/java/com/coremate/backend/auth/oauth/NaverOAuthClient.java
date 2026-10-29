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
 * 네이버 access token 으로 사용자 정보를 조회한다.
 * 카카오와 동일하게 서버 시크릿 없이 토큰만으로 조회하며, 이메일 동의항목은 provider 콘솔 설정에 의존한다.
 * 네이버는 이메일 검증 여부 플래그를 주지 않으므로, 제공된 이메일은 검증된 것으로 본다(로그인 계정 이메일).
 */
@Component
public class NaverOAuthClient implements OAuthClient {

    private static final String USER_INFO_URI = "https://openapi.naver.com/v1/nid/me";
    private static final String SUCCESS_CODE = "00";

    private final RestClient restClient = RestClient.create();

    @Override
    public SocialProvider provider() {
        return SocialProvider.NAVER;
    }

    @Override
    public OAuthUserInfo fetch(OAuthLoginRequest request) {
        String accessToken = request.getAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            throw new CoreMateException(ErrorCode.INVALID_OAUTH_TOKEN);
        }

        NaverMeResponse response;
        try {
            response = restClient.get()
                    .uri(USER_INFO_URI)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(NaverMeResponse.class);
        } catch (RestClientException e) {
            // 토큰 무효·만료·조회 실패는 전부 인증 실패로 본다.
            throw new CoreMateException(ErrorCode.INVALID_OAUTH_TOKEN);
        }

        // 토큰이 유효하지 않으면 HTTP 200 에 resultcode 로 실패를 알리기도 하므로 함께 검사한다.
        if (response == null
                || !SUCCESS_CODE.equals(response.resultcode())
                || response.response() == null
                || response.response().id() == null) {
            throw new CoreMateException(ErrorCode.INVALID_OAUTH_TOKEN);
        }

        NaverAccount account = response.response();
        String email = account.email();
        // 네이버는 검증 플래그가 없다. 이메일이 있으면 검증된 것으로 취급해 기존 계정과 자동 연동한다.
        boolean emailVerified = email != null && !email.isBlank();
        String name = account.nickname() != null ? account.nickname() : account.name();

        return new OAuthUserInfo(account.id(), email, emailVerified, name);
    }

    // 네이버 /v1/nid/me 응답에서 필요한 필드만. JSON 키와 필드명을 맞춰 그대로 바인딩한다.
    private record NaverMeResponse(String resultcode, String message, NaverAccount response) {
    }

    private record NaverAccount(String id, String email, String name, String nickname) {
    }
}
