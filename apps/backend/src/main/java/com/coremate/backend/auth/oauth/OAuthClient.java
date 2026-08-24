package com.coremate.backend.auth.oauth;

import com.coremate.backend.auth.dto.request.OAuthLoginRequest;
import com.coremate.backend.user.entity.SocialProvider;

/**
 * provider가 앱에 발급한 토큰을 검증하고 userinfo를 조회하는 클라이언트.
 * provider마다 한 구현을 두고, OAuthLoginService가 {@link #provider()}로 골라 쓴다.
 */
public interface OAuthClient {

    SocialProvider provider();

    OAuthUserInfo fetch(OAuthLoginRequest request);
}
