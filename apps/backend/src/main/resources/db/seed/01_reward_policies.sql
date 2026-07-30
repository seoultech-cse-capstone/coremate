-- =============================================================================
-- 01_reward_policies.sql
--
-- 보상 정책 시드. 더미가 아니라 서비스 동작에 필요한 정책 데이터다.
-- 이 행이 없으면 RewardService.grantPolicyReward() 가 모든 보상을
-- POLICY_NOT_FOUND 로 건너뛰므로 XP/코인이 하나도 지급되지 않는다.
--
-- 수치 정본: DB 명세서 `reward_policies` 운영 기준 표 (2026-07-30 확정)
--
-- 원칙
--   - 풀이 횟수는 제한하지 않는다. daily_limit 은 "보상 지급 횟수" 상한이다.
--   - XP 는 정답률 무관, 코인은 min_correct_rate + 범위별 최초 1회를 함께 본다.
--   - 코인 일일 한도는 XP 와 같은 숫자를 쓴다(챕터 7 / 과목 5).
--
-- 스키마는 JPA 엔티티(ddl-auto)가 만든다. 이 파일은 INSERT 전용이다.
-- reward_type 이 UNIQUE 라 재실행해도 UPDATE 로 수렴한다(멱등).
-- =============================================================================

-- ---------------------------------------------------------------------------
-- XP 보상 (정답률 무관, 완료 + 일일 한도로만 판정)
-- ---------------------------------------------------------------------------
INSERT INTO reward_policies
    (reward_type, reward_kind, coin_amount, xp_amount, daily_limit, monthly_limit,
     min_correct_rate, first_reward_only, reward_scope, description, is_active, created_at, updated_at)
VALUES
    ('CONCEPT_PRACTICE_XP', 'XP', 0, 10, 10, NULL, NULL, FALSE, 'PER_SESSION',
     '개념 문제집 완료 XP. 하루 10개까지(최대 100 XP)', TRUE, NOW(), NOW()),
    ('CATEGORY_MOCK_XP', 'XP', 0, 30, 7, NULL, NULL, FALSE, 'PER_SESSION',
     '챕터 모의고사(20문항) 완료 XP. 하루 7개까지(최대 210 XP)', TRUE, NOW(), NOW()),
    ('SUBJECT_MOCK_XP', 'XP', 0, 50, 5, NULL, NULL, FALSE, 'PER_SESSION',
     '과목 모의고사(30문항) 완료 XP. 하루 5개까지(최대 250 XP)', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    reward_kind = VALUES(reward_kind), coin_amount = VALUES(coin_amount),
    xp_amount = VALUES(xp_amount), daily_limit = VALUES(daily_limit),
    monthly_limit = VALUES(monthly_limit), min_correct_rate = VALUES(min_correct_rate),
    first_reward_only = VALUES(first_reward_only), reward_scope = VALUES(reward_scope),
    description = VALUES(description), is_active = VALUES(is_active), updated_at = NOW();

-- ---------------------------------------------------------------------------
-- 코인 보상 (정답률 기준 + 범위별 최초 1회 + 일일 한도)
--   일일 한도 숫자는 XP 와 동일하게 맞춘다(챕터 7 / 과목 5).
-- ---------------------------------------------------------------------------
INSERT INTO reward_policies
    (reward_type, reward_kind, coin_amount, xp_amount, daily_limit, monthly_limit,
     min_correct_rate, first_reward_only, reward_scope, description, is_active, created_at, updated_at)
VALUES
    ('CATEGORY_MOCK_FIRST_PASS_COIN', 'COIN', 30, 0, 7, NULL, 70.00, TRUE, 'PER_CATEGORY_LV2',
     '챕터 모의고사 최초 통과 코인. 정답률 70% 이상 + 해당 챕터 최초 1회. 하루 7회까지', TRUE, NOW(), NOW()),
    ('SUBJECT_MOCK_FIRST_PASS_COIN', 'COIN', 100, 0, 5, NULL, 60.00, TRUE, 'PER_SUBJECT',
     '과목 모의고사 최초 통과 코인. 정답률 60% 이상 + 해당 과목 최초 1회. 하루 5회까지', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    reward_kind = VALUES(reward_kind), coin_amount = VALUES(coin_amount),
    xp_amount = VALUES(xp_amount), daily_limit = VALUES(daily_limit),
    monthly_limit = VALUES(monthly_limit), min_correct_rate = VALUES(min_correct_rate),
    first_reward_only = VALUES(first_reward_only), reward_scope = VALUES(reward_scope),
    description = VALUES(description), is_active = VALUES(is_active), updated_at = NOW();

-- ---------------------------------------------------------------------------
-- 오늘의 CS 퀴즈 / 출석 (정답률 무관, 하루 1회)
-- ---------------------------------------------------------------------------
INSERT INTO reward_policies
    (reward_type, reward_kind, coin_amount, xp_amount, daily_limit, monthly_limit,
     min_correct_rate, first_reward_only, reward_scope, description, is_active, created_at, updated_at)
VALUES
    ('DAILY_QUIZ_COMPLETE', 'MIXED', 10, 20, 1, NULL, NULL, TRUE, 'PER_DAY',
     '오늘의 CS 퀴즈 완료 보상. 정답률 무관, 하루 1회', TRUE, NOW(), NOW()),
    ('ATTENDANCE_REWARD', 'MIXED', 5, 10, 1, NULL, NULL, TRUE, 'PER_DAY',
     '출석 보상. 하루 1회', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    reward_kind = VALUES(reward_kind), coin_amount = VALUES(coin_amount),
    xp_amount = VALUES(xp_amount), daily_limit = VALUES(daily_limit),
    monthly_limit = VALUES(monthly_limit), min_correct_rate = VALUES(min_correct_rate),
    first_reward_only = VALUES(first_reward_only), reward_scope = VALUES(reward_scope),
    description = VALUES(description), is_active = VALUES(is_active), updated_at = NOW();

-- ---------------------------------------------------------------------------
-- 레벨업 보상
--   reward_type 이 UNIQUE 라 단일 행으로는 구간별 지급량을 담을 수 없어
--   구간마다 별도 행으로 나눈다(2026-07-30 결정).
--   랜덤박스/특별 아이템 지급은 아이템 마스터가 확정된 뒤 붙인다.
--   여기서는 코인 부분만 정책으로 넣고 reward_kind 는 최종 지급 구성을 반영한다.
-- ---------------------------------------------------------------------------
INSERT INTO reward_policies
    (reward_type, reward_kind, coin_amount, xp_amount, daily_limit, monthly_limit,
     min_correct_rate, first_reward_only, reward_scope, description, is_active, created_at, updated_at)
VALUES
    ('LEVEL_UP_5', 'COIN', 200, 0, NULL, NULL, NULL, TRUE, 'PER_LEVEL',
     '5레벨 달성 보상', TRUE, NOW(), NOW()),
    ('LEVEL_UP_10', 'MIXED', 300, 0, NULL, NULL, NULL, TRUE, 'PER_LEVEL',
     '10레벨 달성 보상. 코인 + 일반 랜덤박스 1개', TRUE, NOW(), NOW()),
    ('LEVEL_UP_20', 'MIXED', 500, 0, NULL, NULL, NULL, TRUE, 'PER_LEVEL',
     '20레벨 달성 보상. 코인 + 일반 랜덤박스 1개', TRUE, NOW(), NOW()),
    ('LEVEL_UP_30', 'MIXED', 1000, 0, NULL, NULL, NULL, TRUE, 'PER_LEVEL',
     '30레벨 달성 보상. 코인 + 상위 랜덤박스 1개 + 특별 아이템', TRUE, NOW(), NOW()),
    ('LEVEL_UP_40', 'COIN', 1000, 0, NULL, NULL, NULL, TRUE, 'PER_LEVEL',
     '40레벨 달성 보상', TRUE, NOW(), NOW()),
    ('LEVEL_UP_50', 'COIN', 1000, 0, NULL, NULL, NULL, TRUE, 'PER_LEVEL',
     '50레벨 달성 보상(레벨 상한)', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    reward_kind = VALUES(reward_kind), coin_amount = VALUES(coin_amount),
    xp_amount = VALUES(xp_amount), daily_limit = VALUES(daily_limit),
    monthly_limit = VALUES(monthly_limit), min_correct_rate = VALUES(min_correct_rate),
    first_reward_only = VALUES(first_reward_only), reward_scope = VALUES(reward_scope),
    description = VALUES(description), is_active = VALUES(is_active), updated_at = NOW();

-- ---------------------------------------------------------------------------
-- 신규 가입 / 광고 / 프리미엄
--   STARTER_PACK, PREMIUM_SIGNUP_BONUS 는 계정당 1회이므로 PER_USER 를 쓴다.
--   스타터팩의 랜덤박스 1개는 아이템 마스터 확정 후 붙인다(현재 코인만).
-- ---------------------------------------------------------------------------
INSERT INTO reward_policies
    (reward_type, reward_kind, coin_amount, xp_amount, daily_limit, monthly_limit,
     min_correct_rate, first_reward_only, reward_scope, description, is_active, created_at, updated_at)
VALUES
    ('STARTER_PACK', 'MIXED', 150, 0, NULL, NULL, NULL, TRUE, 'PER_USER',
     '신규 가입 스타터팩. 코인 150 + 일반 랜덤박스 1개, 계정당 1회', TRUE, NOW(), NOW()),
    ('AD_REWARD', 'COIN', 20, 0, 5, NULL, NULL, FALSE, 'PER_DAY',
     '광고 시청 보상. 1회 20코인, 하루 5회까지(최대 100코인)', TRUE, NOW(), NOW()),
    ('PREMIUM_DAILY_COIN', 'COIN', 50, 0, 1, NULL, NULL, TRUE, 'PER_DAY',
     '프리미엄 구독자 일일 코인. 하루 1회', TRUE, NOW(), NOW()),
    ('PREMIUM_SIGNUP_BONUS', 'COIN', 500, 0, NULL, NULL, NULL, TRUE, 'PER_USER',
     '프리미엄 최초 가입 보너스. 계정당 1회', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    reward_kind = VALUES(reward_kind), coin_amount = VALUES(coin_amount),
    xp_amount = VALUES(xp_amount), daily_limit = VALUES(daily_limit),
    monthly_limit = VALUES(monthly_limit), min_correct_rate = VALUES(min_correct_rate),
    first_reward_only = VALUES(first_reward_only), reward_scope = VALUES(reward_scope),
    description = VALUES(description), is_active = VALUES(is_active), updated_at = NOW();
