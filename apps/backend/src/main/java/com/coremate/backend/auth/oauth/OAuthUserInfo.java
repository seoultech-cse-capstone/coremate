package com.coremate.backend.auth.oauth;

/**
 * provider별 userinfo 응답을 공통 형태로 매핑한 값.
 *
 * @param providerId           provider 고유 subject id(이메일 아님)
 * @param email                제공된 이메일. 동의하지 않으면 null 일 수 있다.
 * @param emailVerified        검증된 이메일인지. false 면 자동 연동하지 않고 신규 생성만 한다.
 * @param name                 제공된 이름/닉네임. 없으면 null. (앱 표시용 닉네임은 서버가 따로 생성한다)
 * @param providerRefreshToken 탈퇴할 때 provider 연결을 끊는 데 쓸 refresh token. 지금은 Apple 만 주고,
 *                             나머지 provider 와 교환에 실패한 Apple 로그인은 null 이다.
 */
public record OAuthUserInfo(String providerId, String email, boolean emailVerified, String name,
                            String providerRefreshToken) {

    public OAuthUserInfo(String providerId, String email, boolean emailVerified, String name) {
        this(providerId, email, emailVerified, name, null);
    }
}
