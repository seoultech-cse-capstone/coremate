-- 04. 랜덤박스 (random_boxes)
-- 담당: 재훈 (B-10)
--
-- ⚠️ 05_shop_products 보다 먼저다. shop_products.random_box_id 가 이 테이블을 참조한다.
--    (분담표에는 04=shop_products / 05=random_boxes 로 적혀 있었으나 FK 순서가 반대여서 맞바꿨다)
--
-- 확정 수치
--   - 박스는 1종. box_grade = STANDARD, required_level = 1 (레벨 제한 없음)
--   - 가격 100 코인, 프리미엄 10% 할인 → 가격/할인은 05_shop_products 에서 정한다
--   - 한 번 열면 보상이 정확히 하나
--
-- 멱등: random_box_id 고정 + ON DUPLICATE KEY UPDATE.

INSERT INTO random_boxes
    (random_box_id, box_name, box_grade, required_level, is_active, created_at, updated_at)
VALUES
    (1, '스탠다드 박스', 'STANDARD', 1, TRUE, NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    box_name = new.box_name, box_grade = new.box_grade,
    required_level = new.required_level, is_active = new.is_active, updated_at = NOW();
