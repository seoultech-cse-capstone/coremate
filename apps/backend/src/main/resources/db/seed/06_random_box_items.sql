-- 06. 랜덤박스 구성 (random_box_items)
-- 담당: 재훈 (B-10, 생성 스크립트는 B-6)
--
-- 선행: 03_avatar_items (item_id FK), 04_random_boxes (random_box_id FK)
--
-- 버킷 확률을 행으로 펼치는 규칙
--   - 행은 아이템 1개당 1행이고, 확률표는 등급 버킷 단위다
--   - probability = 버킷 확률 / 그 등급의 후보 수
--   - 후보 = is_active = TRUE AND is_default = FALSE AND rarity = 해당 등급 (03 기준)
--   - LEGENDARY 는 박스에서 안 나온다 (레벨 30 특별 보상 전용)
--   - 코인은 1행: reward_type = COIN, coin_amount = 30, probability = 0.20
--   - DECIMAL(8,6) 잔차는 각 등급 마지막 행에 몰아주고, 박스 전체 합이 1.000000 이어야 한다
--
-- 버킷 확률 (고정) — 하 0.45 / 중 0.20 / 상 0.15 / 코인 0.20
--   하(COMMON) 14종 → 0.45/14 = 0.032143 (13행) + 잔차 0.032141 (마지막 1행)
--   중(RARE)   10종 → 0.20/10 = 0.020000
--   상(EPIC)    8종 → 0.15/8  = 0.018750
--
-- item_id 는 03 의 auto_increment 결과라 DB 마다 다르므로 하드코딩하지 않고 image_url 로 조회한다.
-- 멱등: 박스 단위로 지웠다가 다시 넣는다. (open_log 는 random_box_items 를 FK 참조하지 않아 안전)

DELETE FROM random_box_items WHERE random_box_id = 1;

INSERT INTO random_box_items
    (random_box_item_id, random_box_id, reward_type, item_id, coin_amount, probability, created_at)
VALUES
    -- 하(COMMON) 14종 — 합 0.450000
    (1,  1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/avatar/hair_long_black.png'),     NULL, 0.032143, NOW()),
    (2,  1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/avatar/eyes_blue.png'),           NULL, 0.032143, NOW()),
    (3,  1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/avatar/mouth_smile_open.png'),    NULL, 0.032143, NOW()),
    (4,  1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/avatar/top_tshirt_graphic.png'),  NULL, 0.032143, NOW()),
    (5,  1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/avatar/top_flannel.png'),         NULL, 0.032143, NOW()),
    (6,  1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/avatar/bottom_jeans.png'),        NULL, 0.032143, NOW()),
    (7,  1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/avatar/bottom_joggers.png'),      NULL, 0.032143, NOW()),
    (8,  1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/avatar/bottom_shorts_socks.png'), NULL, 0.032143, NOW()),
    (9,  1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/avatar/hat_cap.png'),             NULL, 0.032143, NOW()),
    (10, 1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/avatar/hat_backcap.png'),         NULL, 0.032143, NOW()),
    (11, 1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/avatar/glasses_horn.png'),        NULL, 0.032143, NOW()),
    (12, 1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/avatar/shoes_sneakers.png'),      NULL, 0.032143, NOW()),
    (13, 1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/room/furn_shelf.png'),            NULL, 0.032143, NOW()),
    (14, 1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/room/furn_drying_rack.png'),      NULL, 0.032141, NOW()),
    -- 중(RARE) 10종 — 합 0.200000
    (15, 1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/avatar/top_hoodie.png'),          NULL, 0.020000, NOW()),
    (16, 1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/avatar/bottom_slacks.png'),       NULL, 0.020000, NOW()),
    (17, 1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/avatar/hat_beanie.png'),          NULL, 0.020000, NOW()),
    (18, 1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/avatar/shoes_boots.png'),         NULL, 0.020000, NOW()),
    (19, 1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/avatar/bag_backpack.png'),        NULL, 0.020000, NOW()),
    (20, 1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/room/furn_desk_basic.png'),       NULL, 0.020000, NOW()),
    (21, 1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/room/furn_bed_basic.png'),        NULL, 0.020000, NOW()),
    (22, 1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/room/furn_pc_tower.png'),         NULL, 0.020000, NOW()),
    (23, 1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/room/furn_washer.png'),           NULL, 0.020000, NOW()),
    (24, 1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/room/furn_fridge.png'),           NULL, 0.020000, NOW()),
    -- 상(EPIC) 8종 — 합 0.150000
    (25, 1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/avatar/top_hoodie_dev.png'),      NULL, 0.018750, NOW()),
    (26, 1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/avatar/hat_wizard.png'),          NULL, 0.018750, NOW()),
    (27, 1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/avatar/hat_headset.png'),         NULL, 0.018750, NOW()),
    (28, 1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/avatar/glasses_sun.png'),         NULL, 0.018750, NOW()),
    (29, 1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/room/pet_dog.png'),               NULL, 0.018750, NOW()),
    (30, 1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/room/pet_turtle.png'),            NULL, 0.018750, NOW()),
    (31, 1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/room/furn_pet_cushion.png'),      NULL, 0.018750, NOW()),
    (32, 1, 'ITEM', (SELECT item_id FROM avatar_items WHERE image_url = '/assets/room/room_herringbone_wall.png'), NULL, 0.018750, NOW()),
    -- 코인 — 0.200000
    (33, 1, 'COIN', NULL, 30, 0.200000, NOW());
