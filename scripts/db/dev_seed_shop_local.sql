-- ============================================================================
-- 로컬 개발 전용 더미 시드 — 상점 / 랜덤박스 흐름 검증용
-- ============================================================================
-- ⚠️ 로컬 DB 에서만 수동으로 돌린다. db/seed/ 처럼 부팅 시 자동 실행되지 않는다.
--    실제 아이템 목록·이미지(#173, S3)가 확정되면 db/seed/03·04·05·06 에 정본을 넣고
--    이 파일은 쓰지 않는다. (배포로 새지 않게 scripts/db/ 에 둔다)
--
-- 목적
--   - 아직 실제 아바타 아이템·이미지가 없어 db/seed/06(박스 구성)을 못 채우는 상황에서,
--     로컬에서 박스 오픈 / 중복 환급 / 등급별 확률(B-4·B-5·B-7)을 end-to-end 로 검증한다.
--
-- 적용
--   docker exec -i coremate-mysql mysql -ucoremate -pcoremate coremate < scripts/db/dev_seed_shop_local.sql
--
-- 특징
--   - 재실행 안전(멱등): 고정 id + ON DUPLICATE KEY UPDATE. random_box_items 는 박스 단위로 재생성.
--   - 실제 데이터와 안 겹치게 9000 번대 id 를 쓴다(실아이템은 1번대부터).
--   - image_url 은 placeholder. 배포 때는 S3 URL 로 바뀔 뿐 로직은 그대로다.
--   - avatar_items.is_default 컬럼은 아직 없다(용주 1-1 미머지)라 참조하지 않는다.
--     더미는 전부 박스 후보로 취급한다.
--
-- 확률(정본): 하(COMMON) 0.45 / 중(RARE) 0.20 / 상(EPIC) 0.15 / 코인 0.20  → 합 1.000000
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1) 더미 아바타 아이템 9종 (등급별 3개씩). 모두 착용형(비가구)이라 중복 시 환급 경로를 탄다.
-- ---------------------------------------------------------------------------
INSERT INTO avatar_items
    (item_id, item_name, item_type, slot, rarity, image_url, description, is_active, created_at, updated_at)
VALUES
    (9001, '[DEV] 하급 모자',   'HAT',    'HAT',    'COMMON', '/dev/placeholder.png', 'dev dummy', TRUE, NOW(), NOW()),
    (9002, '[DEV] 하급 머리',   'HAIR',   'HAIR',   'COMMON', '/dev/placeholder.png', 'dev dummy', TRUE, NOW(), NOW()),
    (9003, '[DEV] 하급 눈',     'EYES',   'EYES',   'COMMON', '/dev/placeholder.png', 'dev dummy', TRUE, NOW(), NOW()),
    (9004, '[DEV] 중급 상의',   'TOP',    'TOP',    'RARE',   '/dev/placeholder.png', 'dev dummy', TRUE, NOW(), NOW()),
    (9005, '[DEV] 중급 하의',   'BOTTOM', 'BOTTOM', 'RARE',   '/dev/placeholder.png', 'dev dummy', TRUE, NOW(), NOW()),
    (9006, '[DEV] 중급 신발',   'SHOES',  'SHOES',  'RARE',   '/dev/placeholder.png', 'dev dummy', TRUE, NOW(), NOW()),
    (9007, '[DEV] 상급 가방',   'BAG',    'BAG',    'EPIC',   '/dev/placeholder.png', 'dev dummy', TRUE, NOW(), NOW()),
    (9008, '[DEV] 상급 안경',   'EYEWEAR','EYEWEAR','EPIC',   '/dev/placeholder.png', 'dev dummy', TRUE, NOW(), NOW()),
    (9009, '[DEV] 상급 입',     'MOUTH',  'MOUTH',  'EPIC',   '/dev/placeholder.png', 'dev dummy', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    item_name = VALUES(item_name), item_type = VALUES(item_type), slot = VALUES(slot),
    rarity = VALUES(rarity), image_url = VALUES(image_url), description = VALUES(description),
    is_active = VALUES(is_active), updated_at = NOW();

-- ---------------------------------------------------------------------------
-- 2) STANDARD 랜덤박스 1종
-- ---------------------------------------------------------------------------
INSERT INTO random_boxes
    (random_box_id, box_name, box_grade, required_level, is_active, created_at, updated_at)
VALUES
    (9000, '[DEV] 스탠다드 박스', 'STANDARD', 1, TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    box_name = VALUES(box_name), box_grade = VALUES(box_grade),
    required_level = VALUES(required_level), is_active = VALUES(is_active), updated_at = NOW();

-- ---------------------------------------------------------------------------
-- 3) 상점 상품 2종 (랜덤박스 100 / 닉네임 변경권 500)
--    성별 변경권은 성별 구분 폐기(08-28)로 제외한다.
--    변경권은 item_id·random_box_id 가 NULL 이라 avatar_items 를 참조하지 않는다.
-- ---------------------------------------------------------------------------
-- 이전 실행에서 넣었을 수 있는 성별 변경권 더미(9002)를 정리한다.
DELETE FROM shop_products WHERE shop_product_id = 9002;

INSERT INTO shop_products
    (shop_product_id, product_type, item_id, random_box_id, product_name, price_coin, required_level, is_active, created_at, updated_at)
VALUES
    (9000, 'RANDOM_BOX',      NULL, 9000, '[DEV] 랜덤박스',      100, 1, TRUE, NOW(), NOW()),
    (9001, 'NICKNAME_CHANGE', NULL, NULL, '[DEV] 닉네임 변경권', 500, 1, TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    product_type = VALUES(product_type), item_id = VALUES(item_id), random_box_id = VALUES(random_box_id),
    product_name = VALUES(product_name), price_coin = VALUES(price_coin),
    required_level = VALUES(required_level), is_active = VALUES(is_active), updated_at = NOW();

-- ---------------------------------------------------------------------------
-- 4) 랜덤박스 구성 — 버킷 확률을 아이템 행으로 전개 (박스 단위 재생성)
--    하 0.45/3 = 0.15  ·  중 0.20/3 = 0.066667(잔차는 마지막 행)  ·  상 0.15/3 = 0.05  ·  코인 0.20
-- ---------------------------------------------------------------------------
DELETE FROM random_box_items WHERE random_box_id = 9000;

INSERT INTO random_box_items
    (random_box_item_id, random_box_id, reward_type, item_id, coin_amount, probability, created_at)
VALUES
    -- COMMON (합 0.450000)
    (9001, 9000, 'ITEM', 9001, NULL, 0.150000, NOW()),
    (9002, 9000, 'ITEM', 9002, NULL, 0.150000, NOW()),
    (9003, 9000, 'ITEM', 9003, NULL, 0.150000, NOW()),
    -- RARE (합 0.200000, 잔차를 마지막 행에)
    (9004, 9000, 'ITEM', 9004, NULL, 0.066667, NOW()),
    (9005, 9000, 'ITEM', 9005, NULL, 0.066667, NOW()),
    (9006, 9000, 'ITEM', 9006, NULL, 0.066666, NOW()),
    -- EPIC (합 0.150000)
    (9007, 9000, 'ITEM', 9007, NULL, 0.050000, NOW()),
    (9008, 9000, 'ITEM', 9008, NULL, 0.050000, NOW()),
    (9009, 9000, 'ITEM', 9009, NULL, 0.050000, NOW()),
    -- 코인 (0.200000)
    (9010, 9000, 'COIN', NULL, 30,   0.200000, NOW());

-- ---------------------------------------------------------------------------
-- 5) 검증 — total_probability 는 1.000000 이어야 한다.
-- ---------------------------------------------------------------------------
SELECT
    9000                                                             AS random_box_id,
    COUNT(*)                                                         AS row_count,
    SUM(probability)                                                 AS total_probability,
    CASE WHEN SUM(probability) = 1.000000 THEN 'OK' ELSE 'CHECK' END AS status
FROM random_box_items
WHERE random_box_id = 9000;
