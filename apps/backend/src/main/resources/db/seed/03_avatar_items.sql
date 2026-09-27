-- 03. 아바타 아이템 마스터 (avatar_items)
-- 담당: 용주 (Phase 1-11)
--
-- 근거: 「픽셀룸 에셋 템플릿 스펙(통합본)」 5·6절의 실제 에셋 목록.
--       등급 배정은 「아바타, 아이템, 상점 관련 확정 사항」의 버킷 확률(하45/중20/상15)을 따른다.
--
-- 구성 38종
--   기본 지급  6종 (is_default = TRUE, 비매품, 랜덤박스 후보 제외)
--   랜덤박스  32종 = 하 14 + 중 10 + 상 8
--
-- 등급별 아이템 확률 (재훈 B-6 스크립트가 06_random_box_items.sql 을 만들 때 쓰는 값)
--   하 0.45 / 14 = 0.032143   ← 6자리로 안 떨어진다. 13행 0.032143 + 마지막 행 0.032141
--   중 0.20 / 10 = 0.020000
--   상 0.15 /  8 = 0.018750
--   코인 1행      = 0.200000
--   합계          = 1.000000  (하 등급에만 잔차가 남는다. 06 규칙대로 마지막 행에 몰아준다)
--
-- image_url 은 에셋 파일명 기준 placeholder 다. S3 전환은 이슈 #204 에서 한다.
-- 멱등: 같은 item_name 으로 다시 실행해도 값만 갱신된다.

-- ─────────────────────────────────────────────────────────────
-- 기본 지급 6종 — 머리·눈·입·상의·하의·방 테마
-- 신발은 「없음」이 정상 상태라 기본 지급에서 뺐다(스펙 통합본 5절).
-- ─────────────────────────────────────────────────────────────
INSERT INTO avatar_items
    (item_name, item_type, slot, rarity, image_url, description, is_default, metadata, is_active, created_at, updated_at)
VALUES
    ('기본 머리',   'HAIR',      'HAIR',      'COMMON', '/assets/avatar/hair_basic.png',      '가입 시 지급되는 기본 머리',   TRUE, '{"poseVariants":["back","sleep"]}', TRUE, NOW(), NOW()),
    ('기본 눈',     'EYES',      'EYES',      'COMMON', '/assets/avatar/eyes_basic.png',      '가입 시 지급되는 기본 눈',     TRUE, NULL,                               TRUE, NOW(), NOW()),
    ('기본 입',     'MOUTH',     'MOUTH',     'COMMON', '/assets/avatar/mouth_basic.png',     '가입 시 지급되는 기본 입',     TRUE, NULL,                               TRUE, NOW(), NOW()),
    ('기본 상의',   'TOP',       'TOP',       'COMMON', '/assets/avatar/top_basic.png',       '가입 시 지급되는 기본 상의',   TRUE, '{"poseVariants":["sit"]}',          TRUE, NOW(), NOW()),
    ('기본 하의',   'BOTTOM',    'BOTTOM',    'COMMON', '/assets/avatar/bottom_basic.png',    '가입 시 지급되는 기본 하의',   TRUE, '{"poseVariants":["sit"]}',          TRUE, NOW(), NOW()),
    ('베이직 룸',   'WALLPAPER', 'WALLPAPER', 'COMMON', '/assets/room/room_basic_wall.png',   '벽과 바닥이 한 쌍인 기본 방 테마', TRUE,
     '{"wall":"room_basic_wall","floor":"room_basic_floor"}', TRUE, NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    item_type = new.item_type, slot = new.slot, rarity = new.rarity, image_url = new.image_url,
    description = new.description, is_default = new.is_default, metadata = new.metadata,
    is_active = new.is_active, updated_at = NOW();

-- ─────────────────────────────────────────────────────────────
-- 하(COMMON) 14종 — 각 3.2143%
-- ─────────────────────────────────────────────────────────────
INSERT INTO avatar_items
    (item_name, item_type, slot, rarity, image_url, description, is_default, metadata, is_active, created_at, updated_at)
VALUES
    ('긴 검은 머리',    'HAIR',      'HAIR',      'COMMON', '/assets/avatar/hair_long_black.png',      '어깨까지 내려오는 검은 장발', FALSE, '{"poseVariants":["back","sleep"]}', TRUE, NOW(), NOW()),
    ('파란 눈',         'EYES',      'EYES',      'COMMON', '/assets/avatar/eyes_blue.png',            '맑은 파란 눈',               FALSE, NULL,                                TRUE, NOW(), NOW()),
    ('웃는 입',         'MOUTH',     'MOUTH',     'COMMON', '/assets/avatar/mouth_smile_open.png',     '활짝 웃는 입',               FALSE, NULL,                                TRUE, NOW(), NOW()),
    ('그래픽 티셔츠',   'TOP',       'TOP',       'COMMON', '/assets/avatar/top_tshirt_graphic.png',   '프린트가 있는 티셔츠',       FALSE, NULL,                                TRUE, NOW(), NOW()),
    ('플란넬 셔츠',     'TOP',       'TOP',       'COMMON', '/assets/avatar/top_flannel.png',          '체크무늬 플란넬 셔츠',       FALSE, '{"poseVariants":["back"]}',          TRUE, NOW(), NOW()),
    ('청바지',          'BOTTOM',    'BOTTOM',    'COMMON', '/assets/avatar/bottom_jeans.png',         '기본 청바지',               FALSE, '{"poseVariants":["sit"]}',           TRUE, NOW(), NOW()),
    ('조거팬츠',        'BOTTOM',    'BOTTOM',    'COMMON', '/assets/avatar/bottom_joggers.png',       '편한 조거팬츠',             FALSE, '{"poseVariants":["back"]}',          TRUE, NOW(), NOW()),
    ('반바지',          'BOTTOM',    'BOTTOM',    'COMMON', '/assets/avatar/bottom_shorts_socks.png',  '양말까지 있는 반바지',       FALSE, '{"poseVariants":["back"]}',          TRUE, NOW(), NOW()),
    ('야구모자',        'HAT',       'HAT',       'COMMON', '/assets/avatar/hat_cap.png',              '앞으로 쓴 야구모자',         FALSE, '{"poseVariants":["back"]}',          TRUE, NOW(), NOW()),
    ('백캡',            'HAT',       'HAT',       'COMMON', '/assets/avatar/hat_backcap.png',          '뒤로 돌려 쓴 모자',          FALSE, '{"poseVariants":["back"]}',          TRUE, NOW(), NOW()),
    ('뿔테 안경',       'EYEWEAR',   'EYEWEAR',   'COMMON', '/assets/avatar/glasses_horn.png',         '두꺼운 뿔테 안경',           FALSE, NULL,                                TRUE, NOW(), NOW()),
    ('운동화',          'SHOES',     'SHOES',     'COMMON', '/assets/avatar/shoes_sneakers.png',       '기본 운동화',               FALSE, NULL,                                TRUE, NOW(), NOW()),
    ('선반',            'FURNITURE', NULL,        'COMMON', '/assets/room/furn_shelf.png',             '벽에 두는 선반',             FALSE, '{"footprint":{"w":1,"h":1}}',        TRUE, NOW(), NOW()),
    ('빨래 건조대',     'FURNITURE', NULL,        'COMMON', '/assets/room/furn_drying_rack.png',       '접이식 빨래 건조대',         FALSE, '{"footprint":{"w":1,"h":1}}',        TRUE, NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    item_type = new.item_type, slot = new.slot, rarity = new.rarity, image_url = new.image_url,
    description = new.description, is_default = new.is_default, metadata = new.metadata,
    is_active = new.is_active, updated_at = NOW();

-- 구 시드(39종)로 이미 만들어진 '의자' 행 정리. 이 파일은 행을 지우지 않으므로
-- 남아 있으면 계속 랜덤박스 후보(is_active = TRUE)로 잡힌다. user_items 가 참조할
-- 수 있어 DELETE 대신 비활성화한다 — 후보 조건이 is_active = TRUE 라 이걸로 충분하다.
UPDATE avatar_items SET is_active = FALSE, updated_at = NOW() WHERE item_name = '의자';

-- ─────────────────────────────────────────────────────────────
-- 중(RARE) 10종 — 각 2.0%
-- 책상·침대를 여기 둔 이유: 포즈(타이핑·수면)를 유발하는 유일한 가구라
-- 상 등급에 두면 이 기획의 핵심 연출을 오래 못 본다.
-- 책상은 의자까지 묶인 세트 하나다(footprint w=2 는 세트 전체 폭). 침대의 이불과 같아
-- 의자를 별도 아이템으로 등록하지 않는다 — 앱에도 단독 의자를 그릴 자리가 없다.
-- ─────────────────────────────────────────────────────────────
INSERT INTO avatar_items
    (item_name, item_type, slot, rarity, image_url, description, is_default, metadata, is_active, created_at, updated_at)
VALUES
    ('후드티',            'TOP',       'TOP',    'RARE', '/assets/avatar/top_hoodie.png',        '포근한 후드티',            FALSE, '{"poseVariants":["back","sit"]}',              TRUE, NOW(), NOW()),
    ('슬랙스',            'BOTTOM',    'BOTTOM', 'RARE', '/assets/avatar/bottom_slacks.png',     '단정한 슬랙스',            FALSE, '{"poseVariants":["back"]}',                    TRUE, NOW(), NOW()),
    ('비니',              'HAT',       'HAT',    'RARE', '/assets/avatar/hat_beanie.png',        '푹 눌러쓴 비니',           FALSE, '{"poseVariants":["back"]}',                    TRUE, NOW(), NOW()),
    ('부츠',              'SHOES',     'SHOES',  'RARE', '/assets/avatar/shoes_boots.png',       '목이 긴 부츠',             FALSE, NULL,                                          TRUE, NOW(), NOW()),
    ('백팩',              'BAG',       'BAG',    'RARE', '/assets/avatar/bag_backpack.png',      '앞뒤로 나뉘어 그려지는 백팩', FALSE, '{"poseVariants":["back"],"dualPart":true}',  TRUE, NOW(), NOW()),
    ('책상',              'FURNITURE', NULL,     'RARE', '/assets/room/furn_desk_basic.png',     '의자가 딸린 세트. 앉으면 타이핑 포즈를 취한다', FALSE, '{"footprint":{"w":2,"h":1},"interaction":"desk"}', TRUE, NOW(), NOW()),
    ('침대',              'FURNITURE', NULL,     'RARE', '/assets/room/furn_bed_basic.png',      '누우면 잠자는 포즈를 취한다', FALSE, '{"footprint":{"w":2,"h":1},"interaction":"bed"}',  TRUE, NOW(), NOW()),
    ('PC 본체',           'FURNITURE', NULL,     'RARE', '/assets/room/furn_pc_tower.png',       '책상 옆에 두는 PC 본체',    FALSE, '{"footprint":{"w":1,"h":1}}',                  TRUE, NOW(), NOW()),
    ('세탁기',            'FURNITURE', NULL,     'RARE', '/assets/room/furn_washer.png',         '드럼 세탁기',              FALSE, '{"footprint":{"w":1,"h":1}}',                  TRUE, NOW(), NOW()),
    ('냉장고',            'FURNITURE', NULL,     'RARE', '/assets/room/furn_fridge.png',         '2도어 냉장고',             FALSE, '{"footprint":{"w":1,"h":1}}',                  TRUE, NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    item_type = new.item_type, slot = new.slot, rarity = new.rarity, image_url = new.image_url,
    description = new.description, is_default = new.is_default, metadata = new.metadata,
    is_active = new.is_active, updated_at = NOW();

-- ─────────────────────────────────────────────────────────────
-- 상(EPIC) 8종 — 각 1.875%
-- 펫 2종과 펫 방석을 여기 뒀다. 방을 따라다니는 유일한 요소라 체감이 가장 크다.
-- ─────────────────────────────────────────────────────────────
INSERT INTO avatar_items
    (item_name, item_type, slot, rarity, image_url, description, is_default, metadata, is_active, created_at, updated_at)
VALUES
    ('개발자 후드티',   'TOP',       'TOP',       'EPIC', '/assets/avatar/top_hoodie_dev.png',      '로고가 박힌 개발자 후드티',   FALSE, '{"poseVariants":["back"]}',   TRUE, NOW(), NOW()),
    ('마법사 모자',     'HAT',       'HAT',       'EPIC', '/assets/avatar/hat_wizard.png',          '뾰족한 마법사 모자',         FALSE, NULL,                         TRUE, NOW(), NOW()),
    ('헤드셋',          'HAT',       'HAT',       'EPIC', '/assets/avatar/hat_headset.png',         '게이밍 헤드셋',              FALSE, '{"poseVariants":["back"]}',   TRUE, NOW(), NOW()),
    ('선글라스',        'EYEWEAR',   'EYEWEAR',   'EPIC', '/assets/avatar/glasses_sun.png',         '검은 선글라스',              FALSE, NULL,                         TRUE, NOW(), NOW()),
    ('강아지',          'PET',       'PET',       'EPIC', '/assets/room/pet_dog.png',               '방에서 아바타를 따라다닌다',  FALSE, NULL,                         TRUE, NOW(), NOW()),
    ('거북이',          'PET',       'PET',       'EPIC', '/assets/room/pet_turtle.png',            '느긋하게 따라다닌다',        FALSE, NULL,                         TRUE, NOW(), NOW()),
    ('반려동물 방석',   'FURNITURE', NULL,        'EPIC', '/assets/room/furn_pet_cushion.png',      '펫이 쉬어가는 방석',         FALSE, '{"footprint":{"w":1,"h":1}}', TRUE, NOW(), NOW()),
    ('헤링본 룸',       'WALLPAPER', 'WALLPAPER', 'EPIC', '/assets/room/room_herringbone_wall.png', '헤링본 무늬 벽과 바닥 한 쌍', FALSE,
     '{"wall":"room_herringbone_wall","floor":"room_herringbone_floor"}', TRUE, NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    item_type = new.item_type, slot = new.slot, rarity = new.rarity, image_url = new.image_url,
    description = new.description, is_default = new.is_default, metadata = new.metadata,
    is_active = new.is_active, updated_at = NOW();
