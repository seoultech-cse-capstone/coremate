-- =============================================================================
-- V11__add_social_refresh_token.sql
--
-- user_social_accounts 에 refresh_token 을 추가한다.
--
-- Apple 은 Apple 로그인을 넣은 앱에서 사용자가 탈퇴하면 그 사용자의 토큰을 해지해 앱과의 연결을 끊으라고 한다.
-- 해지 요청에는 refresh token 이 필요해서, 로그인할 때 받은 authorization code 를 바꿔 여기 둔다.
-- 지금은 Apple 연결만 채우고 나머지 provider 는 NULL 이다. 탈퇴하면 연결 행과 함께 지운다.
--
-- NULL 허용으로 추가하므로 이 컬럼을 모르는 직전 버전 코드와 같이 돌아도 된다.
-- =============================================================================

ALTER TABLE user_social_accounts
    ADD COLUMN refresh_token VARCHAR(512) DEFAULT NULL;
