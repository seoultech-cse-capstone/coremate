-- =============================================================================
-- V2__add_reward_logs_scope_index.sql
--
-- reward_logs 에 보상 중복 판정용 인덱스를 추가한다.
--
-- 보상을 줄 때마다 RewardLogRepository 가 (user_id, reward_type, target_type, target_id) 로
-- "이미 받았나"를 보고, 「같은 범위 하루 1회」(scope_daily_once, #159)는 여기에 created_at 범위를
-- 더한다. 그런데 이 테이블엔 기본키 말고 인덱스가 없어, 매번 그 사용자의 지급 기록을 전부 훑었다
-- (ddl-auto 는 엔티티에 선언되지 않은 인덱스를 만들지 않았다). DB 명세서 「권장 제약조건」의 인덱스다.
--
-- 온라인 DDL 이라 인덱스를 만드는 동안에도 읽기·쓰기가 막히지 않는다.
-- =============================================================================

CREATE INDEX idx_reward_logs_scope
    ON reward_logs (user_id, reward_type, target_type, target_id, created_at);
