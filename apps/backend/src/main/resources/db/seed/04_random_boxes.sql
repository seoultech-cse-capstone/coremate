-- 04. 랜덤박스 (random_boxes)
-- 담당: 재훈 (B-10)
--
-- ⚠️ 05_shop_products 보다 먼저다. shop_products.random_box_id 가 이 테이블을 참조한다.
--    (분담표에는 04=shop_products / 05=random_boxes 로 적혀 있었으나 FK 순서가 반대여서 맞바꿨다)
--
-- 확정 수치
--   - 박스는 1종. box_grade = STANDARD, required_level = 1 (레벨 제한 없음)
--   - 가격 100 코인, 프리미엄 10% 할인
--   - 한 번 열면 보상이 정확히 하나

-- ⚠️ 아래 한 줄을 지우지 말 것.
--    Spring 의 ScriptUtils 는 주석만 있는 파일을 거절한다:
--      IllegalArgumentException: 'script' must not be null or empty
--    continue-on-error: false 라서 앱이 아예 기동하지 않는다.
--    실제 INSERT 를 넣을 때 이 줄도 같이 지운다.
SELECT 1;
