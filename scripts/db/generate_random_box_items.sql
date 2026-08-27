-- ============================================================================
-- 랜덤박스 구성(random_box_items) 버킷 전개 생성 스크립트  (B-6)
-- ============================================================================
-- 확정 확률표는 "등급 버킷" 단위인데 random_box_items 는 "아이템 1개당 1행"이다.
-- 이 스크립트가 그 사이를 잇는다 — avatar_items 의 후보를 읽어 버킷 확률을
-- 아이템 행으로 펼쳐 STANDARD 박스의 구성을 다시 만든다.
--
-- 특징
--   - 아이템이 0개여도 실행된다. 그 경우 코인 1행(0.20)만 생기고, 아래 검증 쿼리의
--     total_probability 가 1.000000 에 못 미쳐 "아직 아이템이 안 꽂혔음"을 알려준다.
--   - 재실행 안전: 대상 박스의 기존 구성 행을 지우고 다시 만든다(regenerate).
--
-- 선행
--   - random_boxes 에 box_grade = 'STANDARD' 박스가 있어야 한다 (db/seed/04_random_boxes.sql)
--   - avatar_items 에 is_default 컬럼이 있어야 한다 (용주 1-1). 아이템 목록·등급이
--     확정되면(#173) 이 스크립트를 돌려 나온 결과를 db/seed/06_random_box_items.sql 로 옮긴다(B-10).
--
-- 버킷 규칙
--   - 버킷 확률(고정): 하(COMMON) 0.45 / 중(RARE) 0.20 / 상(EPIC) 0.15 / 코인 0.20
--   - 아이템별 probability = 버킷 확률 / 그 등급의 후보 수  (예: 하 9개면 각 0.45/9 = 0.05)
--   - 후보 = is_active = TRUE AND is_default = FALSE AND rarity = 해당 등급
--   - LEGENDARY 는 박스에서 안 나온다(레벨 30 특별 보상 전용)이라 후보에서 제외
--   - 코인은 1행: reward_type = COIN, coin_amount = 30, probability = 0.20
--   - DECIMAL(8,6) 반올림 잔차는 각 등급 "마지막 행"에 몰아주어 박스 전체 합이 1.000000
-- ============================================================================

-- 대상: STANDARD 박스 1종.
SET @box_id = (
    SELECT random_box_id
    FROM random_boxes
    WHERE box_grade = 'STANDARD'
    ORDER BY random_box_id
    LIMIT 1
);

-- 기존 구성 제거 후 재생성.
DELETE FROM random_box_items
WHERE random_box_id = @box_id;

-- 아이템 보상 행 — 버킷 확률을 후보 수로 나눠 펼치고, 잔차는 각 등급 마지막 행에 몰아준다.
INSERT INTO random_box_items (random_box_id, reward_type, item_id, coin_amount, probability, created_at)
SELECT
    @box_id,
    'ITEM',
    cand.item_id,
    NULL,
    CASE
        -- 각 등급의 마지막 행: 버킷 합에서 앞선 행들의 반올림 합을 뺀 잔차.
        WHEN cand.rn = cand.cnt
            THEN bucket.bucket_prob - ROUND(bucket.bucket_prob / cand.cnt, 6) * (cand.cnt - 1)
        ELSE ROUND(bucket.bucket_prob / cand.cnt, 6)
    END,
    NOW()
FROM (
    SELECT
        ai.item_id,
        ai.rarity,
        ROW_NUMBER() OVER (PARTITION BY ai.rarity ORDER BY ai.item_id) AS rn,
        COUNT(*)     OVER (PARTITION BY ai.rarity)                     AS cnt
    FROM avatar_items ai
    WHERE ai.is_active = TRUE
      AND ai.is_default = FALSE
      AND ai.rarity IN ('COMMON', 'RARE', 'EPIC')
) AS cand
JOIN (
             SELECT 'COMMON' AS rarity, 0.45 AS bucket_prob
    UNION ALL SELECT 'RARE',            0.20
    UNION ALL SELECT 'EPIC',            0.15
) AS bucket ON bucket.rarity = cand.rarity;

-- 코인 보상 행 — 아이템 수와 무관하게 항상 1행.
INSERT INTO random_box_items (random_box_id, reward_type, item_id, coin_amount, probability, created_at)
VALUES (@box_id, 'COIN', NULL, 30, 0.200000, NOW());

-- 검증 — total_probability 는 1.000000 이어야 한다.
-- (아이템이 아직 없으면 0.200000 만 나온다. 아이템 확정 후 다시 돌리면 1.000000 이 된다.)
SELECT
    @box_id                                                              AS random_box_id,
    COUNT(*)                                                             AS row_count,
    SUM(probability)                                                     AS total_probability,
    CASE WHEN SUM(probability) = 1.000000 THEN 'OK' ELSE 'CHECK' END     AS status
FROM random_box_items
WHERE random_box_id = @box_id;
