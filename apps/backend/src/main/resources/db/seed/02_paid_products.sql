-- =============================================================================
-- 02_paid_products.sql
--
-- 유료 기능 가격 정책과 프리미엄 구독 상품 시드.
-- 수치 정본: DB 명세서 `feature_access_products` / `subscription_plans` 운영 기준 (2026-07-30 확정)
--
-- 01_reward_policies.sql 과 같은 규칙을 따른다.
--   - INSERT 전용(스키마는 JPA 엔티티가 만든다)
--   - UNIQUE 키 기반 upsert 라 재실행해도 중복되지 않는다
-- =============================================================================

-- ---------------------------------------------------------------------------
-- AI 약점 분석표 생성 가격
--
-- 100 코인은 "하루 제대로 학습하면 분석표 1개" 에 해당한다(학습으로 하루에 얻는
-- 코인이 100 전후). 월간 한도가 사용자당 AI 호출 비용의 상한 장치다.
-- ---------------------------------------------------------------------------
INSERT INTO feature_access_products
    (feature_type, target_type, product_name, price_coin, premium_price_coin,
     access_policy, free_user_monthly_limit, premium_monthly_limit, is_active, created_at, updated_at)
VALUES
    ('WEAKNESS_ANALYSIS_CREATE', 'ANALYSIS_CREATE_REQUEST', 'AI 약점 분석표 생성',
     100, 50, 'CREATE_ONCE', 5, 30, TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    target_type = VALUES(target_type), product_name = VALUES(product_name),
    price_coin = VALUES(price_coin), premium_price_coin = VALUES(premium_price_coin),
    access_policy = VALUES(access_policy),
    free_user_monthly_limit = VALUES(free_user_monthly_limit),
    premium_monthly_limit = VALUES(premium_monthly_limit),
    is_active = VALUES(is_active), updated_at = NOW();

-- ---------------------------------------------------------------------------
-- 프리미엄 구독
--
-- price_krw 는 NULL 로 둔다. 스토어 수수료·부가세·PG 정책이 정해져야 확정할 수 있다.
-- 코인 혜택은 확정값이라 먼저 넣는다.
-- ---------------------------------------------------------------------------
INSERT INTO subscription_plans
    (plan_code, plan_name, price_krw, daily_coin, signup_bonus_coin, monthly_bonus_coin,
     random_box_discount_rate, is_active, created_at, updated_at)
VALUES
    ('PREMIUM_MONTHLY', 'CoreMate 프리미엄', NULL, 50, 500, 300, 10, TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    plan_name = VALUES(plan_name), price_krw = VALUES(price_krw),
    daily_coin = VALUES(daily_coin), signup_bonus_coin = VALUES(signup_bonus_coin),
    monthly_bonus_coin = VALUES(monthly_bonus_coin),
    random_box_discount_rate = VALUES(random_box_discount_rate),
    is_active = VALUES(is_active), updated_at = NOW();
