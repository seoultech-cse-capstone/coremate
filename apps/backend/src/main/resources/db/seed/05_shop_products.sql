-- 05. 상점 상품 (shop_products)
-- 담당: 재훈 (B-10)
--
-- 선행: 03_avatar_items (item_id FK), 04_random_boxes (random_box_id FK)
--
-- 확정 수치 — 2종. 꾸미기 아이템 직판은 폐기됐고, 성별 변경권은 성별 구분 폐기(08-28)로 제외한다.
--   - 랜덤박스      100 코인 (random_box_id 참조, item_id NULL)
--   - 닉네임 변경권  500 코인 (1회용, item_id·random_box_id 모두 NULL)
--
-- 멱등: shop_product_id 고정 + ON DUPLICATE KEY UPDATE.

INSERT INTO shop_products
    (shop_product_id, product_type, item_id, random_box_id, product_name, price_coin, required_level, is_active, created_at, updated_at)
VALUES
    (1, 'RANDOM_BOX',      NULL, 1,    '랜덤박스',      100, 1, TRUE, NOW(), NOW()),
    (2, 'NICKNAME_CHANGE', NULL, NULL, '닉네임 변경권', 500, 1, TRUE, NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    product_type = new.product_type, item_id = new.item_id, random_box_id = new.random_box_id,
    product_name = new.product_name, price_coin = new.price_coin,
    required_level = new.required_level, is_active = new.is_active, updated_at = NOW();
