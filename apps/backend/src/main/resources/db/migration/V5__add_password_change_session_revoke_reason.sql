-- =============================================================================
-- V5__add_password_change_session_revoke_reason.sql
--
-- user_sessions.revoke_reason 에 PASSWORD_CHANGE 를 추가한다.
--
-- 로그인 상태에서 비밀번호를 바꾸면 지금 기기만 남기고 다른 기기를 해지한다(#250). 그 사유로
-- 남기는 값이다. 이 컬럼은 MySQL native enum 이라 SessionRevokeReason 에 상수만 추가하면
-- 저장할 때 "Data truncated" 로 실패한다(validate 는 enum 목록을 비교하지 않는다).
--
-- 기존 값과 NULL 허용은 V1 정의 그대로 두고 새 값만 끝에 붙인다. 끝에 붙이면 테이블을 다시
-- 쓰지 않는다(메타데이터만 바뀐다).
--
-- 되돌릴 때 주의: 이 값이 한 번이라도 저장된 뒤 앱만 직전 버전으로 되돌리면, 직전 버전은 이
-- 상수를 몰라 그 행을 읽을 때 실패한다(그 사용자의 로그인 포함). 그때는 해당 행을
-- PASSWORD_RESET 으로 바꾼다. 둘 다 "안내 없음" 사유라 사용자에게 보이는 차이가 없다.
-- =============================================================================

ALTER TABLE user_sessions
    MODIFY COLUMN revoke_reason ENUM('MAX_SESSION_EXCEEDED', 'PASSWORD_RESET', 'REMOTE_LOGOUT', 'USER_LOGOUT',
        'WITHDRAWAL', 'PASSWORD_CHANGE') DEFAULT NULL;
