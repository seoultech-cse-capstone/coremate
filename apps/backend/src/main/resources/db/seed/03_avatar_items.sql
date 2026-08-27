-- 03. 아바타 아이템 마스터 (avatar_items)
-- 담당: 용주 (Phase 1-11)
--
-- 🚧 아직 비어 있다. 아이템 목록과 상/중/하 등급 배정이 확정돼야 채울 수 있다.
--    이 파일이 나와야 05_shop_products / 06_random_box_items 의 FK 가 성립한다.
--
-- 채울 때 지켜야 할 것
--   - 멱등: INSERT ... ON DUPLICATE KEY UPDATE (매 기동마다 실행된다)
--   - item_type 은 13종, slot 은 12종. FURNITURE 만 slot = NULL
--   - 기본 지급 8종(머리/눈/입/상의/하의/신발/벽지/바닥)은 is_default = TRUE 비매품.
--     랜덤박스 후보에서 제외된다
--   - rarity 는 COMMON / RARE / EPIC / LEGENDARY

-- ⚠️ 아래 한 줄을 지우지 말 것.
--    Spring 의 ScriptUtils 는 주석만 있는 파일을 거절한다:
--      IllegalArgumentException: 'script' must not be null or empty
--    continue-on-error: false 라서 앱이 아예 기동하지 않는다.
--    실제 INSERT 를 넣을 때 이 줄도 같이 지운다.
SELECT 1;
