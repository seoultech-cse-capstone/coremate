-- =============================================================================
-- V3__set_scope_daily_once_default.sql
--
-- reward_policies.scope_daily_once 에 기본값 FALSE 를 준다.
--
-- 2026-08-11(#159)에 나중에 붙은 NOT NULL 컬럼인데 기본값이 없다. 앱(JPA)은 늘 값을 넣어 문제가
-- 없지만, SQL 로 정책 행을 직접 넣으면서 이 컬럼을 빼먹으면 "doesn't have a default value" 로
-- 실패한다. 빠졌을 때 FALSE(하루 1회 제한 없음)로 보는 것이 이 플래그가 생기기 전 동작과 같다.
--
-- 기본값만 바꾸는 변경이라 테이블을 다시 쓰지 않는다(메타데이터만 바뀐다).
-- =============================================================================

ALTER TABLE reward_policies ALTER COLUMN scope_daily_once SET DEFAULT b'0';
