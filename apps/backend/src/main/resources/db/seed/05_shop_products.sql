-- 05. 상점 상품 (shop_products)
-- 담당: 재훈 (B-10)
--
-- 선행: 03_avatar_items (item_id FK), 04_random_boxes (random_box_id FK)
--
-- 확정 수치 — 3종뿐이다. 꾸미기 아이템 직판은 폐기됐다
--   - 랜덤박스      100 코인
--   - 닉네임 변경권  500 코인 (1회용)
--   - 성별 변경권    500 코인 (1회용, base_avatar_type 교체)

-- ⚠️ 아래 한 줄을 지우지 말 것.
--    Spring 의 ScriptUtils 는 주석만 있는 파일을 거절한다:
--      IllegalArgumentException: 'script' must not be null or empty
--    continue-on-error: false 라서 앱이 아예 기동하지 않는다.
--    실제 INSERT 를 넣을 때 이 줄도 같이 지운다.
SELECT 1;
