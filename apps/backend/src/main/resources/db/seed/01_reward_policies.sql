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
--   - 중복 판정 축이 둘이다. first_reward_only = 범위별 "평생" 1회,
--     scope_daily_once = 같은 범위 "하루" 1회(2026-08-11 신설). 서로 독립이다.
--
-- 스키마는 JPA 엔티티(ddl-auto)가 만든다. 이 파일은 INSERT 전용이다.
-- reward_type 이 UNIQUE 라 재실행해도 UPDATE 로 수렴한다(멱등).
-- =============================================================================

-- ---------------------------------------------------------------------------
-- [0] 선언되지 않은 정책 비활성화
--
-- 이 파일이 "현재 살아 있는 정책의 전부"가 되게 만드는 단계다.
-- 아래 목록에 없는 reward_type 은 is_active = FALSE 로 끈다.
-- RewardService 는 findByRewardTypeAndActiveTrue 로 조회하므로 꺼진 정책은
-- 즉시 무시되고, 해당 보상은 POLICY_NOT_FOUND 로 스킵된다.
--
-- 행을 DELETE 하지 않는 이유: reward_logs / xp_logs 의 reward_policy_id 가
-- "이 지급이 어떤 정책 때문이었는지"를 가리킨다. 지우면 과거 지급 근거가 끊긴다.
-- (DR-REWARD-06 의 "기존 정책은 비활성화하고 새 정책을 추가" 방침과 같다.)
--
-- ⚠️ 로컬·개발 단계 전용이다. 시드 목록에 없는 정책은 예외 없이 꺼지므로,
--    DB 에서 직접 만든 정책도 다음 기동에 비활성화된다.
--    AR-ADMIN-04(관리자가 reward_policies 를 관리) 기능이 생기거나 운영에
--    배포할 때는 이 [0] 단계를 제거해야 한다. 관리자가 화면에서 만든 정책이
--    배포·재기동 때 조용히 꺼지기 때문이다.
--    배포 준비 시 Flyway 로 옮기면서 함께 정리할 항목이다.
-- ---------------------------------------------------------------------------
UPDATE reward_policies
   SET is_active = FALSE, updated_at = NOW()
 WHERE is_active = TRUE
   AND reward_type NOT IN (
       'CONCEPT_PRACTICE_XP', 'CATEGORY_MOCK_XP', 'SUBJECT_MOCK_XP',
       'CATEGORY_MOCK_FIRST_PASS_COIN', 'SUBJECT_MOCK_FIRST_PASS_COIN',
       'CATEGORY_MOCK_RETRY_COIN', 'SUBJECT_MOCK_RETRY_COIN',
       'DAILY_QUIZ_COMPLETE', 'ATTENDANCE_REWARD',
       'LEVEL_UP_5', 'LEVEL_UP_10', 'LEVEL_UP_20',
       'LEVEL_UP_30', 'LEVEL_UP_40', 'LEVEL_UP_50',
       'STARTER_PACK', 'AD_REWARD', 'PREMIUM_DAILY_COIN', 'PREMIUM_SIGNUP_BONUS'
   );

-- ---------------------------------------------------------------------------
-- XP 보상 (정답률 무관, 완료 + 일일 한도로 판정)
--
-- 모의고사 XP 2종은 scope_daily_once = TRUE 다(2026-08-11 결정). 같은 챕터를 하루에
-- 일곱 번 다시 풀어 210 XP 를 캐는 경로를 막는다. 하루 한도 숫자는 그대로라
-- 하루 상한 590 XP 는 변하지 않고, 채우려면 서로 다른 범위를 풀어야 할 뿐이다.
--
-- ⚠️ 이 두 정책의 first_reward_only 는 FALSE 를 유지해야 한다. 호출부가 이제
--    target 을 넘기므로, 켜는 순간 "범위당 평생 XP 1회" 로 조용히 바뀐다.
--
-- 개념 문제집은 적용 대상이 아니다(2026-08-11 결정). 복습 성격이 강해 같은 개념을
-- 다시 푸는 것을 막을 이유가 약하다고 봤다.
-- ---------------------------------------------------------------------------
INSERT INTO reward_policies
    (reward_type, reward_kind, coin_amount, xp_amount, daily_limit,
     min_correct_rate, first_reward_only, scope_daily_once, reward_scope, description, is_active, created_at, updated_at)
VALUES
    ('CONCEPT_PRACTICE_XP', 'XP', 0, 10, 10, NULL, FALSE, FALSE, 'PER_SESSION',
     '개념 문제집 완료 XP. 하루 10개까지(최대 100 XP)', TRUE, NOW(), NOW()),
    ('CATEGORY_MOCK_XP', 'XP', 0, 30, 7, NULL, FALSE, TRUE, 'PER_SESSION',
     '챕터 모의고사(20문항) 완료 XP. 하루 7개까지(최대 210 XP)', TRUE, NOW(), NOW()),
    ('SUBJECT_MOCK_XP', 'XP', 0, 50, 5, NULL, FALSE, TRUE, 'PER_SESSION',
     '과목 모의고사(30문항) 완료 XP. 하루 5개까지(최대 250 XP)', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    reward_kind = VALUES(reward_kind), coin_amount = VALUES(coin_amount),
    xp_amount = VALUES(xp_amount), daily_limit = VALUES(daily_limit),
    min_correct_rate = VALUES(min_correct_rate),
    first_reward_only = VALUES(first_reward_only), scope_daily_once = VALUES(scope_daily_once),
    reward_scope = VALUES(reward_scope),
    description = VALUES(description), is_active = VALUES(is_active), updated_at = NOW();

-- ---------------------------------------------------------------------------
-- 코인 보상 - 최초 통과 (정답률 기준 + 범위별 최초 1회 + 일일 한도)
--   일일 한도 숫자는 XP 와 동일하게 맞춘다(챕터 7 / 과목 5).
-- ---------------------------------------------------------------------------
INSERT INTO reward_policies
    (reward_type, reward_kind, coin_amount, xp_amount, daily_limit,
     min_correct_rate, first_reward_only, scope_daily_once, reward_scope, description, is_active, created_at, updated_at)
VALUES
    -- reward_scope 를 NULL 로 둔다. 챕터 모의고사는 대분류(PER_CATEGORY_LV1)와
    -- 중분류(PER_CATEGORY_LV2) 두 범위를 함께 쓰므로 한 값으로 고를 수 없다.
    -- 실제 중복 판정은 SolveRewardService 가 세션 유형에 따라 target_type 을 정해서 한다.
    -- 정책을 하나로 유지하는 이유는 일일 한도 7회를 대분류/중분류가 "함께" 써야 하기 때문이다.
    -- 타입을 둘로 쪼개면 하루 7 + 7 = 14회가 되어 확정 정책과 어긋난다.
    ('CATEGORY_MOCK_FIRST_PASS_COIN', 'COIN', 30, 0, 7, 70.00, TRUE, FALSE, NULL,
     '챕터 모의고사 최초 통과 코인. 정답률 70% 이상 + 해당 챕터 최초 1회. 하루 7회까지(대분류/중분류 합산). 중복 판정 범위는 회차 유형에 따라 대분류 또는 중분류', TRUE, NOW(), NOW()),
    ('SUBJECT_MOCK_FIRST_PASS_COIN', 'COIN', 100, 0, 5, 60.00, TRUE, FALSE, 'PER_SUBJECT',
     '과목 모의고사 최초 통과 코인. 정답률 60% 이상 + 해당 과목 최초 1회. 하루 5회까지', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    reward_kind = VALUES(reward_kind), coin_amount = VALUES(coin_amount),
    xp_amount = VALUES(xp_amount), daily_limit = VALUES(daily_limit),
    min_correct_rate = VALUES(min_correct_rate),
    first_reward_only = VALUES(first_reward_only), scope_daily_once = VALUES(scope_daily_once),
    reward_scope = VALUES(reward_scope),
    description = VALUES(description), is_active = VALUES(is_active), updated_at = NOW();

-- ---------------------------------------------------------------------------
-- 코인 보상 - 재응시 (정답률 기준 + 일일 한도. 범위별 중복 판정 없음)
--
-- 최초 통과 코인은 범위마다 평생 1회라, 156개 챕터 범위와 7개 과목을 모두
-- 소진하면 학습으로 얻는 코인이 0 이 된다. 그 뒤의 반복 수입을 담당하는 정책이다.
-- MVP 에는 광고 보상(AD_REWARD)이 들어가지 않으므로, 소진 후 무과금 사용자의
-- 영구 수입은 출석 5 + 오늘의 퀴즈 10 = 하루 15 코인뿐이었다.
--
-- first_reward_only 를 FALSE 로 두는 게 최초 통과 정책과의 유일한 구조적 차이다.
-- 정답률 기준은 최초 통과와 같은 값을 쓴다. 기준을 낮추면 쉬운 회차를 반복해
-- 코인을 캐는 경로가 열린다.
--
-- 대신 scope_daily_once = TRUE 다(2026-08-11 결정). 평생 반복은 되지만 같은 챕터·과목으로는
-- 하루 한 번뿐이다. 최초 통과 정책은 이미 범위별 평생 1회라 이 플래그가 필요 없다.
--
-- ⚠️ 일일 한도는 최초 통과 정책과 "별도로" 쌓인다. daily_reward_limits 가
--    reward_type 단위로 카운트하기 때문이다. 이론상 하루 판정 횟수는 챕터 14회
--    (최초 7 + 재응시 7)까지 늘 수 있으나, 그러려면 챕터 280문항을 정답률
--    기준을 넘겨 풀어야 해서 실질 상한은 기존과 같다(2026-08-07 결정).
--    scope_daily_once 판정은 이 테이블이 아니라 reward_logs 로 한다. 여기는
--    reward_type 단위 총량 카운터라 "어느 범위로 받았는지"를 담지 못한다.
--
-- 금액은 문항당 단가를 맞춘 값이다. 챕터 20문항에 5(0.25/문항),
-- 과목 30문항에 10(0.33/문항). 과목만 반복하는 쪽이 유리해지지 않게 한다.
-- ---------------------------------------------------------------------------
INSERT INTO reward_policies
    (reward_type, reward_kind, coin_amount, xp_amount, daily_limit,
     min_correct_rate, first_reward_only, scope_daily_once, reward_scope, description, is_active, created_at, updated_at)
VALUES
    ('CATEGORY_MOCK_RETRY_COIN', 'COIN', 5, 0, 7, 70.00, FALSE, TRUE, 'PER_SESSION',
     '챕터 모의고사 재응시 코인. 정답률 70% 이상 + 해당 챕터 최초 보상을 이미 받은 경우. 하루 7회까지(최초 통과 한도와 별도)', TRUE, NOW(), NOW()),
    ('SUBJECT_MOCK_RETRY_COIN', 'COIN', 10, 0, 5, 60.00, FALSE, TRUE, 'PER_SESSION',
     '과목 모의고사 재응시 코인. 정답률 60% 이상 + 해당 과목 최초 보상을 이미 받은 경우. 하루 5회까지(최초 통과 한도와 별도)', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    reward_kind = VALUES(reward_kind), coin_amount = VALUES(coin_amount),
    xp_amount = VALUES(xp_amount), daily_limit = VALUES(daily_limit),
    min_correct_rate = VALUES(min_correct_rate),
    first_reward_only = VALUES(first_reward_only), scope_daily_once = VALUES(scope_daily_once),
    reward_scope = VALUES(reward_scope),
    description = VALUES(description), is_active = VALUES(is_active), updated_at = NOW();

-- ---------------------------------------------------------------------------
-- 오늘의 CS 퀴즈 / 출석 (정답률 무관, 하루 1회)
-- ---------------------------------------------------------------------------
INSERT INTO reward_policies
    (reward_type, reward_kind, coin_amount, xp_amount, daily_limit,
     min_correct_rate, first_reward_only, scope_daily_once, reward_scope, description, is_active, created_at, updated_at)
VALUES
    ('DAILY_QUIZ_COMPLETE', 'MIXED', 10, 20, 1, NULL, TRUE, FALSE, 'PER_DAY',
     '오늘의 CS 퀴즈 완료 보상. 정답률 무관, 하루 1회', TRUE, NOW(), NOW()),
    ('ATTENDANCE_REWARD', 'MIXED', 5, 10, 1, NULL, TRUE, FALSE, 'PER_DAY',
     '출석 보상. 하루 1회', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    reward_kind = VALUES(reward_kind), coin_amount = VALUES(coin_amount),
    xp_amount = VALUES(xp_amount), daily_limit = VALUES(daily_limit),
    min_correct_rate = VALUES(min_correct_rate),
    first_reward_only = VALUES(first_reward_only), scope_daily_once = VALUES(scope_daily_once),
    reward_scope = VALUES(reward_scope),
    description = VALUES(description), is_active = VALUES(is_active), updated_at = NOW();

-- ---------------------------------------------------------------------------
-- 레벨업 보상
--   reward_type 이 UNIQUE 라 단일 행으로는 구간별 지급량을 담을 수 없어
--   구간마다 별도 행으로 나눈다(2026-07-30 결정).
--   랜덤박스/특별 아이템 지급은 아이템 마스터가 확정된 뒤 붙인다.
--   여기서는 코인 부분만 정책으로 넣고 reward_kind 는 최종 지급 구성을 반영한다.
-- ---------------------------------------------------------------------------
INSERT INTO reward_policies
    (reward_type, reward_kind, coin_amount, xp_amount, daily_limit,
     min_correct_rate, first_reward_only, scope_daily_once, reward_scope, description, is_active, created_at, updated_at)
VALUES
    ('LEVEL_UP_5', 'COIN', 200, 0, NULL, NULL, TRUE, FALSE, 'PER_LEVEL',
     '5레벨 달성 보상', TRUE, NOW(), NOW()),
    ('LEVEL_UP_10', 'MIXED', 300, 0, NULL, NULL, TRUE, FALSE, 'PER_LEVEL',
     '10레벨 달성 보상. 코인 + 일반 랜덤박스 1개', TRUE, NOW(), NOW()),
    ('LEVEL_UP_20', 'MIXED', 500, 0, NULL, NULL, TRUE, FALSE, 'PER_LEVEL',
     '20레벨 달성 보상. 코인 + 일반 랜덤박스 1개', TRUE, NOW(), NOW()),
    ('LEVEL_UP_30', 'MIXED', 1000, 0, NULL, NULL, TRUE, FALSE, 'PER_LEVEL',
     '30레벨 달성 보상. 코인 + 상위 랜덤박스 1개 + 특별 아이템', TRUE, NOW(), NOW()),
    ('LEVEL_UP_40', 'COIN', 1000, 0, NULL, NULL, TRUE, FALSE, 'PER_LEVEL',
     '40레벨 달성 보상', TRUE, NOW(), NOW()),
    ('LEVEL_UP_50', 'COIN', 1000, 0, NULL, NULL, TRUE, FALSE, 'PER_LEVEL',
     '50레벨 달성 보상(레벨 상한)', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    reward_kind = VALUES(reward_kind), coin_amount = VALUES(coin_amount),
    xp_amount = VALUES(xp_amount), daily_limit = VALUES(daily_limit),
    min_correct_rate = VALUES(min_correct_rate),
    first_reward_only = VALUES(first_reward_only), scope_daily_once = VALUES(scope_daily_once),
    reward_scope = VALUES(reward_scope),
    description = VALUES(description), is_active = VALUES(is_active), updated_at = NOW();

-- ---------------------------------------------------------------------------
-- 신규 가입 / 광고 / 프리미엄
--   STARTER_PACK, PREMIUM_SIGNUP_BONUS 는 계정당 1회이므로 PER_USER 를 쓴다.
--   스타터팩의 랜덤박스 1개는 아이템 마스터 확정 후 붙인다(현재 코인만).
-- ---------------------------------------------------------------------------
INSERT INTO reward_policies
    (reward_type, reward_kind, coin_amount, xp_amount, daily_limit,
     min_correct_rate, first_reward_only, scope_daily_once, reward_scope, description, is_active, created_at, updated_at)
VALUES
    ('STARTER_PACK', 'MIXED', 150, 0, NULL, NULL, TRUE, FALSE, 'PER_USER',
     '신규 가입 스타터팩. 코인 150 + 일반 랜덤박스 1개, 계정당 1회', TRUE, NOW(), NOW()),
    ('AD_REWARD', 'COIN', 20, 0, 5, NULL, FALSE, FALSE, 'PER_DAY',
     '광고 시청 보상. 1회 20코인, 하루 5회까지(최대 100코인)', TRUE, NOW(), NOW()),
    ('PREMIUM_DAILY_COIN', 'COIN', 50, 0, 1, NULL, TRUE, FALSE, 'PER_DAY',
     '프리미엄 구독자 일일 코인. 하루 1회', TRUE, NOW(), NOW()),
    ('PREMIUM_SIGNUP_BONUS', 'COIN', 500, 0, NULL, NULL, TRUE, FALSE, 'PER_USER',
     '프리미엄 최초 가입 보너스. 계정당 1회', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    reward_kind = VALUES(reward_kind), coin_amount = VALUES(coin_amount),
    xp_amount = VALUES(xp_amount), daily_limit = VALUES(daily_limit),
    min_correct_rate = VALUES(min_correct_rate),
    first_reward_only = VALUES(first_reward_only), scope_daily_once = VALUES(scope_daily_once),
    reward_scope = VALUES(reward_scope),
    description = VALUES(description), is_active = VALUES(is_active), updated_at = NOW();
