-- =============================================================================
-- R__02_paid_products.sql
--
-- 유료 기능 가격 정책과 프리미엄 구독 상품 시드.
-- 수치 정본: DB 명세서 `feature_access_products` / `subscription_plans` 운영 기준 (2026-07-30 확정)
--
-- R__01_reward_policies.sql 과 같은 규칙을 따른다.
--   - 데이터만 넣는다(스키마는 V 마이그레이션이 만든다)
--   - UNIQUE 키 기반 upsert 라 다시 실행돼도 중복되지 않는다
-- =============================================================================

-- ---------------------------------------------------------------------------
-- AI 약점 분석표 생성 가격
--
-- 100 코인은 "하루 제대로 학습하면 분석표 1개" 에 해당한다(학습으로 하루에 얻는
-- 코인이 100 전후).
--
-- 월간 생성 한도는 두지 않는다(2026-08-11 결정). 코인 가격과 학습으로 얻는 하루 코인
-- 수입 상한이 이미 생성 빈도의 상한 역할을 하므로 이중 장치를 두지 않는다.
-- ---------------------------------------------------------------------------
INSERT INTO feature_access_products
    (feature_type, target_type, product_name, price_coin, premium_price_coin,
     access_policy, is_active, created_at, updated_at)
VALUES
    ('WEAKNESS_ANALYSIS_CREATE', 'ANALYSIS_CREATE_REQUEST', 'AI 약점 분석표 생성',
     100, 50, 'CREATE_ONCE', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    target_type = VALUES(target_type), product_name = VALUES(product_name),
    price_coin = VALUES(price_coin), premium_price_coin = VALUES(premium_price_coin),
    access_policy = VALUES(access_policy),
    is_active = VALUES(is_active), updated_at = NOW();

-- ---------------------------------------------------------------------------
-- 프리미엄 구독
--
-- price_krw 는 NULL 로 둔다. 스토어 수수료·부가세·PG 정책이 정해져야 확정할 수 있다.
-- 코인 혜택은 확정값이라 먼저 넣는다.
--
-- 가입 보너스(500코인)는 여기가 아니라 reward_policies 의 PREMIUM_SIGNUP_BONUS 행에 있다.
-- 분석표 할인가(50)는 feature_access_products 에 있다. 월간 생성 한도는 두지 않는다.
-- 한 값이 두 테이블에 있으면 어느 쪽이 정본인지 모호해지므로 나누지 않는다.
-- ---------------------------------------------------------------------------
INSERT INTO subscription_plans
    (plan_code, plan_name, price_krw, daily_coin_amount, monthly_bonus_coin,
     random_box_discount_rate, description, is_active, created_at, updated_at)
VALUES
    ('PREMIUM_MONTHLY', 'CoreMate 프리미엄', NULL, 50, 300, 10,
     '일일 50코인, 월간 보너스 300코인, 랜덤박스 10% 할인. 분석표 할인가는 feature_access_products 참조',
     TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    plan_name = VALUES(plan_name), price_krw = VALUES(price_krw),
    daily_coin_amount = VALUES(daily_coin_amount),
    monthly_bonus_coin = VALUES(monthly_bonus_coin),
    random_box_discount_rate = VALUES(random_box_discount_rate),
    description = VALUES(description),
    is_active = VALUES(is_active), updated_at = NOW();
