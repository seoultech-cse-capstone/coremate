-- =============================================================================
-- V10__add_apple_social_provider.sql
--
-- user_social_accounts.provider 에 APPLE 을 추가한다.
--
-- 아이폰 앱에 카카오·네이버·구글 로그인을 넣으면 App Store 심사 지침(4.8)상 Apple 로그인도 함께 넣어야 한다.
-- 이 컬럼은 MySQL native enum 이라 SocialProvider 에 상수만 추가하면 저장할 때 "Data truncated" 로
-- 실패한다(validate 는 enum 목록을 비교하지 않는다).
--
-- 기존 값과 NOT NULL 은 V1 정의 그대로 두고 새 값만 끝에 붙인다. 끝에 붙이면 테이블을 다시 쓰지 않는다.
--
-- 되돌릴 때 주의: 앱만 직전 버전으로 되돌려도 다른 provider 로그인은 그대로 된다. 직전 버전은 APPLE 행을
-- 조회하지 않기 때문이다. Apple 로그인만 막힌다.
-- =============================================================================

ALTER TABLE user_social_accounts
    MODIFY COLUMN provider ENUM('GOOGLE', 'KAKAO', 'NAVER', 'APPLE') NOT NULL;
