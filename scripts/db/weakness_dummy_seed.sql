-- =============================================================================
-- weakness_dummy_seed.sql
--
-- 목적: CoreMate "사용자 약점 분석표" 기능을 실제 MySQL 데이터로 손검증하기 위한
--       자기완결형(throwaway) 더미 시드. 약점 분석에 필요한 전체 사슬
--       (users -> user_currency -> 콘텐츠 트리 -> 완료 solve 세션 -> 문항별 로그)을
--       고정 ID로 심는다. WRONG / LONG_TIME(=expected 2배) / HINT_USED 신호가
--       개념별로 골고루 생기도록 구성했다. (dev 전용, 운영 DB에 넣지 말 것)
--
-- !! 선행 조건 !!
--   이 스크립트는 CREATE TABLE 을 하지 않는다(INSERT 전용).
--   스키마는 백엔드가 기동할 때 Flyway 마이그레이션이 생성하므로,
--   반드시 백엔드를 dev 프로파일로 "한 번" 기동해 테이블이 만들어진 뒤 실행해야 한다.
--     예) SPRING_PROFILES_ACTIVE=dev ./gradlew :backend:bootRun   (테이블 생성 후 종료)
--
-- 실행 방법(예):
--   mysql -u coremate -p coremate < scripts/db/weakness_dummy_seed.sql
--   (DB=coremate / user=coremate — apps/backend/src/main/resources/application.yaml 기준)
--
-- 재실행(idempotent):
--   - 콘텐츠/솔브 더미는 고정 PK(9000번대)로 심고, 맨 위에서 그 범위를 DELETE 후 재INSERT 한다.
--   - users / user_currency 는 파괴를 피하려 INSERT ... ON DUPLICATE KEY UPDATE(upsert)로 처리한다.
--   - FK 순서 부담을 줄이려 전체를 SET FOREIGN_KEY_CHECKS=0 ... =1 로 감싼다.
--
-- 참고(약점 신호 판정 기준 — weakness/support/WeaknessDevDataSeeder.java 와 동일):
--   WRONG      : is_correct = 0
--   LONG_TIME  : is_correct = 1 AND time_spent_sec >= expected_solve_sec_snapshot * 2
--   HINT_USED  : hint_used = 1 AND hint_count >= 1
--
-- 고정 ID 맵:
--   user_id=1
--   subject_id=9001
--   category_lv1_id=9101
--   category_lv2_id=9201(이진 트리), 9202(힙)
--   concept_id=9301(완전 이진 트리),9302(포화 이진 트리),9303(힙의 성질),9304(힙 연산)
--   common_mistake_id=9401~9404 (개념별 1개)
--   question_id=9501~9520 (개념당 5문항, 총 20)
--   solve_session_id=9601(SUBJECT_MOCK),9602(CATEGORY_LV2_MOCK/이진트리),9603(CATEGORY_LV2_MOCK/힙)
--   solve_log_id=9701~9720
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 0) 기존 더미 범위 정리 (자식 -> 부모 순; FK_CHECKS=0 라 순서 부담은 적음)
--    questions 9501~9520 을 참조하는 다른 세션의 로그까지 지워야 question 삭제가 안전.
-- -----------------------------------------------------------------------------
DELETE FROM solve_logs
 WHERE solve_session_id BETWEEN 9601 AND 9603
    OR question_id BETWEEN 9501 AND 9520;
DELETE FROM solve_sessions WHERE solve_session_id BETWEEN 9601 AND 9603;
DELETE FROM question       WHERE question_id       BETWEEN 9501 AND 9520;
DELETE FROM common_mistake WHERE common_mistake_id BETWEEN 9401 AND 9412;
DELETE FROM concept        WHERE concept_id        BETWEEN 9301 AND 9304;
DELETE FROM category_lv2   WHERE category_lv2_id   BETWEEN 9201 AND 9202;
DELETE FROM category_lv1   WHERE category_lv1_id   = 9101;
DELETE FROM subject        WHERE subject_id        = 9001;

-- =============================================================================
-- 1) 사용자 (테이블: users)
--    NN: nickname, role, level, total_xp, school_verified, status, created_at, updated_at
--    role/status 는 enum STRING (UserRole.USER / UserStatus.ACTIVE)
-- =============================================================================
INSERT INTO users
    (user_id, email, password_hash, nickname, role, level, total_xp,
     school_name, school_email, school_verified, status,
     created_at, updated_at, last_login_at)
VALUES
    (1, 'weakness.dev@coremate.test', '{noop}dev-dummy-hash', '[DEV]약점분석유저',
     'USER', 5, 1200, '서울과학기술대학교', NULL, 0, 'ACTIVE',
     NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE
    email          = VALUES(email),
    password_hash  = VALUES(password_hash),
    nickname       = VALUES(nickname),
    role           = VALUES(role),
    level          = VALUES(level),
    total_xp       = VALUES(total_xp),
    school_name    = VALUES(school_name),
    school_verified= VALUES(school_verified),
    status         = VALUES(status),
    updated_at     = NOW();

-- =============================================================================
-- 2) 재화 (테이블: user_currency) — created_at 없음, updated_at 만 존재
--    코인 넉넉히(1000): 약점분석표 생성 시 코인 차감 테스트용.
-- =============================================================================
INSERT INTO user_currency (user_id, coin, updated_at)
VALUES (1, 1000, NOW())
ON DUPLICATE KEY UPDATE
    coin       = VALUES(coin),
    updated_at = NOW();

-- =============================================================================
-- 3) 콘텐츠 트리 (같은 과목)
-- =============================================================================

-- 3-1) subject
--   NN: subject_code(uq), subject_name(uq), sort_order, is_active, created_at, updated_at
INSERT INTO subject
    (subject_id, subject_code, subject_name, subject_name_en, sort_order, is_active,
     created_at, updated_at)
VALUES
    -- is_active=0: 개발 시나리오 전용 과목이라 사용자 화면(문제집 목록·진행률·친구 프로필
    -- 레이더)에 노출되면 안 된다. 백엔드 조회는 모두 active 필터를 타므로 이 값만으로 가려진다.
    -- 이미 쌓인 solve_sessions·solve_logs 는 그대로 남아 약점분석 개발 데이터는 유지된다.
    (9001, 'DEV_DS_WEAKNESS', '[DEV] 자료구조(약점분석)', 'Data Structures (DEV)', 90, 0,
     NOW(), NOW());

-- 3-2) category_lv1 (subject_id FK NN)
--   NN: subject_id, category_lv1_code, category_lv1_name, sort_order, is_active, created_at, updated_at
INSERT INTO category_lv1
    (category_lv1_id, subject_id, category_lv1_code, category_lv1_name, category_lv1_name_en,
     description, sort_order, is_active, created_at, updated_at)
VALUES
    (9101, 9001, 'DEV_DS_TREE', '트리', 'Tree',
     '트리 계열 자료구조 대분류(더미)', 1, 1, NOW(), NOW());

-- 3-3) category_lv2 x2 (category_lv1_id FK NN)
--   NN: category_lv1_id, category_lv2_code, category_lv2_name, sort_order, is_active, created_at, updated_at
INSERT INTO category_lv2
    (category_lv2_id, category_lv1_id, category_lv2_code, category_lv2_name, category_lv2_name_en,
     description, sort_order, is_active, created_at, updated_at)
VALUES
    (9201, 9101, 'DEV_DS_BINTREE', '이진 트리', 'Binary Tree', '이진 트리 중분류(더미)', 1, 1, NOW(), NOW()),
    (9202, 9101, 'DEV_DS_HEAP',    '힙',       'Heap',        '힙 중분류(더미)',       2, 1, NOW(), NOW());

-- 3-4) concept x4 (category_lv2_id FK NN)
--   NN: concept_code(uq), category_lv2_id, concept_name, one_line_definition, core_summary,
--       core_keywords_json, key_properties_json, sort_order, review_status, is_active,
--       version_no, created_at, updated_at
--   (aliases_json/source_refs/review_note/approved_at 는 nullable — approved_at 는 승인 표현으로 채움)
--   review_status 는 String 컬럼: 소문자 'approved' 사용(ConceptDetailResponse @Schema example="approved" 관례).
INSERT INTO concept
    (concept_id, concept_code, category_lv2_id, concept_name, concept_name_en, aliases_json,
     one_line_definition, core_summary, core_keywords_json, key_properties_json, source_refs,
     sort_order, review_status, review_note, is_active, version_no, created_at, updated_at, approved_at)
VALUES
    (9301, 'DEV_CPT_COMPLETE_BINTREE', 9201, '완전 이진 트리', 'Complete Binary Tree',
     '["완전이진트리","complete tree"]',
     '마지막 레벨을 제외한 모든 레벨이 꽉 차 있고 마지막 레벨은 왼쪽부터 채워진 이진 트리.',
     '완전 이진 트리는 배열 표현이 자연스러워 힙 구현의 기반이 된다.',
     '["완전 이진 트리","배열 표현","마지막 레벨"]',
     '["마지막 레벨을 제외하고 꽉 참","마지막 레벨은 왼쪽부터 채움","배열 인덱스 계산이 쉬움"]',
     NULL, 1, 'approved', NULL, 1, 1, NOW(), NOW(), NOW()),

    (9302, 'DEV_CPT_FULL_BINTREE', 9201, '포화 이진 트리', 'Perfect Binary Tree',
     '["포화이진트리","perfect tree","full tree"]',
     '모든 내부 노드가 두 자식을 갖고 모든 리프가 같은 레벨에 있는 이진 트리.',
     '포화 이진 트리는 높이 h에서 노드 수가 2^(h+1)-1 로 결정된다.',
     '["포화 이진 트리","모든 리프 동일 레벨","노드 수 공식"]',
     '["모든 리프가 같은 레벨","완전 이진 트리의 특수 경우","노드 수 2^(h+1)-1"]',
     NULL, 2, 'approved', NULL, 1, 1, NOW(), NOW(), NOW()),

    (9303, 'DEV_CPT_HEAP_PROP', 9202, '힙의 성질', 'Heap Property',
     '["힙성질","heap property"]',
     '부모-자식 간 부분 순서(최대 힙: 부모>=자식)를 보장하는 완전 이진 트리 기반 구조의 성질.',
     '힙은 전체 정렬이 아니라 부분 순서만 보장한다.',
     '["최대 힙","부분 순서","완전 이진 트리 기반"]',
     '["부모가 자식보다 크거나 같음(최대 힙)","형제 간 순서 미보장","루트가 최댓값/최솟값"]',
     NULL, 1, 'approved', NULL, 1, 1, NOW(), NOW(), NOW()),

    (9304, 'DEV_CPT_HEAP_OPS', 9202, '힙 연산', 'Heap Operations',
     '["힙연산","heap operations","sift"]',
     '힙의 삽입(sift-up)/삭제(sift-down) 및 build-heap 등 힙 성질을 유지하는 연산.',
     '삽입·삭제는 O(log n), build-heap 은 O(n).',
     '["sift-up","sift-down","build-heap"]',
     '["삽입은 sift-up 으로 성질 회복","루트 삭제는 sift-down","임의 탐색은 O(n)"]',
     NULL, 2, 'approved', NULL, 1, 1, NOW(), NOW(), NOW());

-- 3-5) common_mistake x4 (concept_id FK NN) — 힌트용 correction_text 포함
--   NN: concept_id, mistake_title, mistake_text, correction_text, review_status, is_active, created_at, updated_at
INSERT INTO common_mistake
    (common_mistake_id, concept_id, mistake_title, mistake_text, correction_text,
     review_status, review_note, is_active, created_at, updated_at)
VALUES
    (9401, 9301, '완전 이진 트리를 ''모든 레벨 꽉 참''으로 오해',
     '완전 이진 트리를 포화 이진 트리처럼 모든 레벨이 완전히 채워져야 한다고 생각한다.',
     '마지막 레벨을 제외한 레벨만 꽉 차면 되고, 마지막 레벨은 왼쪽부터 부분적으로 채워도 완전 이진 트리다.',
     'approved', NULL, 1, NOW(), NOW()),
    (9402, 9302, '포화와 완전 이진 트리 혼동',
     '포화 이진 트리와 완전 이진 트리를 같은 것으로 본다.',
     '포화는 모든 리프가 같은 레벨인 특수한 경우다. 완전이라고 해서 포화인 것은 아니다.',
     'approved', NULL, 1, NOW(), NOW()),
    (9403, 9303, '힙이 완전 정렬됐다고 오해',
     '힙이 배열 전체가 정렬된 구조라고 생각한다.',
     '힙은 부모-자식 간 부분 순서만 보장하며 형제 간 순서나 전체 정렬은 보장하지 않는다.',
     'approved', NULL, 1, NOW(), NOW()),
    (9404, 9304, '힙 탐색이 빠르다고 오해',
     '힙에서 임의 값 탐색이 O(log n)이라 생각한다.',
     '힙은 최댓값/최솟값 접근만 O(1)이고 임의 원소 탐색은 O(n)이다. 삽입·삭제가 O(log n).',
     'approved', NULL, 1, NOW(), NOW()),
    -- 개념당 오개념을 3개로 늘려 문항 진단 프로필의 오답→오개념 매핑률을 높인다
    -- (실제 콘텐츠는 개념당 4개 내외라 더미도 그에 맞춘다).
    (9405, 9301, '배열 표현의 인덱스 계산을 혼동',
     '완전 이진 트리를 배열로 표현할 때 부모/자식/마지막 내부 노드 인덱스 계산을 헷갈린다.',
     '1-기반 배열에서 자식은 2i, 2i+1 이고 마지막 내부 노드는 floor(n/2) 이다.',
     'approved', NULL, 1, NOW(), NOW()),
    (9406, 9301, '구조 조건과 값 정렬을 혼동',
     '완전 이진 트리라는 구조 조건을 값이 정렬돼 있다는 뜻으로 받아들인다.',
     '완전 이진 트리는 노드가 채워지는 모양에 대한 조건이며 값의 순서와는 무관하다.',
     'approved', NULL, 1, NOW(), NOW()),
    (9407, 9302, '포화 이진 트리의 노드 수 공식을 잘못 적용',
     '높이 h인 포화 이진 트리의 노드 수를 2h 나 h^2 같은 식으로 계산한다.',
     '높이 h 포화 이진 트리의 노드 수는 2^(h+1)-1 이다.',
     'approved', NULL, 1, NOW(), NOW()),
    (9408, 9302, '포화 이진 트리의 리프 구조를 오해',
     '포화 이진 트리에 리프가 없거나 리프 레벨이 서로 다르다고 생각한다.',
     '포화 이진 트리는 모든 리프가 같은 레벨에 있고 내부 노드는 항상 두 자식을 갖는다.',
     'approved', NULL, 1, NOW(), NOW()),
    (9409, 9303, '힙을 이진 탐색 트리와 혼동',
     '힙에도 왼쪽 < 부모 < 오른쪽 같은 좌우 대소 관계가 성립한다고 생각한다.',
     '힙은 부모-자식 간 대소만 보장하며 형제나 좌우 사이에는 순서가 없다. 그건 이진 탐색 트리의 성질이다.',
     'approved', NULL, 1, NOW(), NOW()),
    (9410, 9303, '힙의 배열 인덱스 규칙을 혼동',
     '1-기반 배열 힙에서 자식 인덱스를 i-1, i/2, 2i+2 등으로 잘못 계산한다.',
     '1-기반 배열에서 왼쪽 자식은 2i, 오른쪽 자식은 2i+1, 부모는 floor(i/2) 이다.',
     'approved', NULL, 1, NOW(), NOW()),
    (9411, 9304, '힙 연산이 전체 정렬을 만든다고 오해',
     'sift-up/sift-down 이 트리 전체를 정렬 상태로 만든다고 생각한다.',
     '두 연산은 힙 성질(부모-자식 부분 순서)만 회복하며 전체 정렬을 만들지 않는다.',
     'approved', NULL, 1, NOW(), NOW()),
    (9412, 9304, 'sift-up 과 sift-down 의 쓰임을 혼동',
     '삽입과 삭제에 어떤 연산이 쓰이는지 반대로 알거나 둘을 같은 것으로 본다.',
     '삽입은 마지막에 넣고 sift-up, 루트 삭제는 마지막 원소를 올린 뒤 sift-down 이다.',
     'approved', NULL, 1, NOW(), NOW());

-- 3-6) question x20 (concept_id FK NN, hint_common_mistake_id FK nullable -> 개념별 mistake 연결)
--   NN: concept_id, question_format, question_type, question_text, correct_answer, explanation_text,
--       difficulty_level(1~5), difficulty_source, review_status, source_type,
--       is_daily_quiz_eligible, is_mock_eligible, is_concept_practice_eligible, is_active,
--       version_no, created_at, updated_at
--   choices_json: JSON 배열 [{"key","text"}] (solve/dailyquiz 파서 형식). correct_answer = key(대문자 A~D).
--   content_hash: unique -> 충돌 방지 위해 문항별 distinct 더미값.
INSERT INTO question
    (question_id, concept_id, hint_common_mistake_id, question_format, question_type, question_text,
     choices_json, correct_answer, explanation_text, difficulty_level, difficulty_source,
     expected_solve_sec, content_hash, review_status, review_note, source_type, source_refs,
     is_daily_quiz_eligible, is_mock_eligible, is_concept_practice_eligible, is_active,
     version_no, created_at, updated_at, approved_at)
VALUES
    -- 개념 9301 완전 이진 트리 (expected 60, difficulty 3)
    (9501, 9301, 9401, 'MCQ_4', 'definition', '완전 이진 트리(complete binary tree)의 정의로 가장 적절한 것은?',
     '[{"key":"A","text":"마지막 레벨을 제외한 모든 레벨이 꽉 차 있고, 마지막 레벨은 왼쪽부터 채워진다."},{"key":"B","text":"모든 레벨이 예외 없이 꽉 차 있다."},{"key":"C","text":"모든 노드의 차수가 2이다."},{"key":"D","text":"왼쪽 서브트리가 항상 오른쪽보다 높다."}]',
     'A', '완전 이진 트리는 마지막 레벨만 왼쪽부터 부분적으로 채워도 된다.', 3, 'ai_initial',
     60, 'devhash-weakness-9501', 'approved', NULL, 'ai_generated', NULL, 1, 1, 1, 1, 1, NOW(), NOW(), NOW()),
    (9502, 9301, 9401, 'MCQ_4', 'comparison', '완전 이진 트리와 포화 이진 트리의 차이로 옳은 것은?',
     '[{"key":"A","text":"둘은 완전히 동일한 개념이다."},{"key":"B","text":"포화는 모든 레벨이 꽉 차지만, 완전은 마지막 레벨만 왼쪽부터 채워도 된다."},{"key":"C","text":"완전 이진 트리가 항상 포화보다 노드가 많다."},{"key":"D","text":"포화 이진 트리는 노드 수가 홀수여야 한다."}]',
     'B', '포화는 완전의 특수한 경우다.', 3, 'ai_initial',
     60, 'devhash-weakness-9502', 'approved', NULL, 'ai_generated', NULL, 1, 1, 1, 1, 1, NOW(), NOW(), NOW()),
    (9503, 9301, 9401, 'MCQ_4', 'application', '노드가 6개인 완전 이진 트리를 1-기반 배열로 표현할 때 마지막 내부(비리프) 노드의 인덱스는?',
     '[{"key":"A","text":"1"},{"key":"B","text":"2"},{"key":"C","text":"3"},{"key":"D","text":"6"}]',
     'C', 'n=6 에서 마지막 내부 노드 인덱스는 floor(6/2)=3.', 3, 'ai_initial',
     60, 'devhash-weakness-9503', 'approved', NULL, 'ai_generated', NULL, 1, 1, 1, 1, 1, NOW(), NOW(), NOW()),
    (9504, 9301, 9401, 'MCQ_4', 'misconception', '완전 이진 트리에 대한 흔한 오해로 옳은 것은?',
     '[{"key":"A","text":"높이가 항상 1이다."},{"key":"B","text":"리프가 하나뿐이다."},{"key":"C","text":"항상 값이 정렬되어 있다."},{"key":"D","text":"''모든 레벨이 꽉 차야 한다''고 오해하기 쉽지만 마지막 레벨은 왼쪽부터 부분적으로 채워도 된다."}]',
     'D', '완전 이진 트리를 포화로 오해하는 대표적 케이스.', 3, 'ai_initial',
     60, 'devhash-weakness-9504', 'approved', NULL, 'ai_generated', NULL, 1, 1, 1, 1, 1, NOW(), NOW(), NOW()),
    (9505, 9301, 9401, 'MCQ_4', 'example', '다음 중 완전 이진 트리의 예로 적절한 것은?',
     '[{"key":"A","text":"마지막 레벨 노드가 왼쪽부터 빈틈없이 채워진 트리"},{"key":"B","text":"오른쪽 자식만 있는 편향 트리"},{"key":"C","text":"마지막 레벨 중간에 빈 자리가 있는 트리"},{"key":"D","text":"루트만 있는데 자식 포인터가 3개인 트리"}]',
     'A', '마지막 레벨이 왼쪽부터 연속으로 채워지면 완전 이진 트리.', 3, 'ai_initial',
     60, 'devhash-weakness-9505', 'approved', NULL, 'ai_generated', NULL, 1, 1, 1, 1, 1, NOW(), NOW(), NOW()),

    -- 개념 9302 포화 이진 트리 (expected 90, difficulty 4)
    (9506, 9302, 9402, 'MCQ_4', 'definition', '포화 이진 트리(perfect binary tree)의 정의로 옳은 것은?',
     '[{"key":"A","text":"마지막 레벨만 채워진다."},{"key":"B","text":"모든 내부 노드가 두 자식을 갖고 모든 리프가 같은 레벨에 있다."},{"key":"C","text":"노드가 한 개뿐이다."},{"key":"D","text":"차수가 3인 노드가 있다."}]',
     'B', '모든 리프가 같은 레벨에 있는 것이 핵심.', 4, 'ai_initial',
     90, 'devhash-weakness-9506', 'approved', NULL, 'ai_generated', NULL, 1, 1, 1, 1, 1, NOW(), NOW(), NOW()),
    (9507, 9302, 9402, 'MCQ_4', 'application', '높이가 h인 포화 이진 트리의 노드 수는?',
     '[{"key":"A","text":"h"},{"key":"B","text":"2h"},{"key":"C","text":"2^(h+1) - 1"},{"key":"D","text":"h^2"}]',
     'C', '포화 이진 트리 노드 수 공식은 2^(h+1)-1.', 4, 'ai_initial',
     90, 'devhash-weakness-9507', 'approved', NULL, 'ai_generated', NULL, 1, 1, 1, 1, 1, NOW(), NOW(), NOW()),
    (9508, 9302, 9402, 'MCQ_4', 'comparison', '포화 이진 트리는 완전 이진 트리이기도 한가?',
     '[{"key":"A","text":"아니다, 서로 배타적이다."},{"key":"B","text":"경우에 따라 다르며 알 수 없다."},{"key":"C","text":"포화면 완전이 될 수 없다."},{"key":"D","text":"모든 포화 이진 트리는 완전 이진 트리이기도 하다."}]',
     'D', '포화는 완전의 부분집합이다.', 4, 'ai_initial',
     90, 'devhash-weakness-9508', 'approved', NULL, 'ai_generated', NULL, 1, 1, 1, 1, 1, NOW(), NOW(), NOW()),
    (9509, 9302, 9402, 'MCQ_4', 'misconception', '포화 이진 트리에 대한 오해로 옳은 것은?',
     '[{"key":"A","text":"''완전 이진 트리면 곧 포화''라고 착각하기 쉬우나 완전이 포화인 것은 아니다."},{"key":"B","text":"포화는 리프가 없다."},{"key":"C","text":"포화는 항상 노드가 짝수다."},{"key":"D","text":"포화는 높이가 0이다."}]',
     'A', '완전과 포화의 포함 관계를 반대로 오해하는 경우.', 4, 'ai_initial',
     90, 'devhash-weakness-9509', 'approved', NULL, 'ai_generated', NULL, 1, 1, 1, 1, 1, NOW(), NOW(), NOW()),
    (9510, 9302, 9402, 'MCQ_4', 'example', '다음 중 포화 이진 트리의 예로 적절한 것은?',
     '[{"key":"A","text":"마지막 레벨에 빈 자리가 있는 트리"},{"key":"B","text":"높이 2에서 노드가 7개로 모두 채워진 트리"},{"key":"C","text":"편향 트리"},{"key":"D","text":"노드 5개짜리 트리"}]',
     'B', '높이 2 포화 이진 트리 노드 수는 2^3-1=7.', 4, 'ai_initial',
     90, 'devhash-weakness-9510', 'approved', NULL, 'ai_generated', NULL, 1, 1, 1, 1, 1, NOW(), NOW(), NOW()),

    -- 개념 9303 힙의 성질 (expected 45, difficulty 2)
    (9511, 9303, 9403, 'MCQ_4', 'definition', '최대 힙(max-heap)의 힙 성질로 옳은 것은?',
     '[{"key":"A","text":"왼쪽 < 루트 < 오른쪽 이다."},{"key":"B","text":"모든 리프의 값이 같다."},{"key":"C","text":"모든 부모 노드 값이 자식 노드 값보다 크거나 같다."},{"key":"D","text":"전체가 정렬된 배열이다."}]',
     'C', '최대 힙은 부모>=자식 부분 순서만 보장.', 2, 'ai_initial',
     45, 'devhash-weakness-9511', 'approved', NULL, 'ai_generated', NULL, 1, 1, 1, 1, 1, NOW(), NOW(), NOW()),
    (9512, 9303, 9403, 'MCQ_4', 'misconception', '힙에 대한 흔한 오해로 옳은 것은?',
     '[{"key":"A","text":"힙은 항상 정렬되어 있다."},{"key":"B","text":"힙은 이진 탐색 트리다."},{"key":"C","text":"루트가 최소이자 최대다."},{"key":"D","text":"''힙이 완전히 정렬돼 있다''는 오해가 흔하지만 힙은 부분 순서만 보장한다."}]',
     'D', '힙은 전체 정렬을 보장하지 않는다.', 2, 'ai_initial',
     45, 'devhash-weakness-9512', 'approved', NULL, 'ai_generated', NULL, 1, 1, 1, 1, 1, NOW(), NOW(), NOW()),
    (9513, 9303, 9403, 'MCQ_4', 'comparison', '최대 힙과 이진 탐색 트리(BST)의 차이로 옳은 것은?',
     '[{"key":"A","text":"BST는 중위순회가 정렬되지만, 힙은 형제 간 순서를 보장하지 않는다."},{"key":"B","text":"둘은 같은 자료구조다."},{"key":"C","text":"힙은 탐색이 항상 O(log n) 보장된다."},{"key":"D","text":"BST는 항상 완전 이진 트리다."}]',
     'A', 'BST는 정렬 순회, 힙은 부분 순서.', 2, 'ai_initial',
     45, 'devhash-weakness-9513', 'approved', NULL, 'ai_generated', NULL, 1, 1, 1, 1, 1, NOW(), NOW(), NOW()),
    (9514, 9303, 9403, 'MCQ_4', 'application', '1-기반 배열로 표현한 힙에서 인덱스 i의 왼쪽 자식 인덱스는?',
     '[{"key":"A","text":"i-1"},{"key":"B","text":"2i"},{"key":"C","text":"i/2"},{"key":"D","text":"2i+2"}]',
     'B', '1-기반 배열 힙의 왼쪽 자식은 2i.', 2, 'ai_initial',
     45, 'devhash-weakness-9514', 'approved', NULL, 'ai_generated', NULL, 1, 1, 1, 1, 1, NOW(), NOW(), NOW()),
    (9515, 9303, 9403, 'MCQ_4', 'example', '다음 중 유효한 최대 힙 배열은?',
     '[{"key":"A","text":"[1,2,3]"},{"key":"B","text":"[3,5,4]"},{"key":"C","text":"[9,7,8,3,6]"},{"key":"D","text":"[2,9,1]"}]',
     'C', '[9,7,8,3,6] 은 모든 부모>=자식 을 만족.', 2, 'ai_initial',
     45, 'devhash-weakness-9515', 'approved', NULL, 'ai_generated', NULL, 1, 1, 1, 1, 1, NOW(), NOW(), NOW()),

    -- 개념 9304 힙 연산 (expected 60, difficulty 3)
    (9516, 9304, 9404, 'MCQ_4', 'definition', '힙 삽입 후 상향 이동(sift-up)의 목적으로 옳은 것은?',
     '[{"key":"A","text":"트리를 완전히 정렬한다."},{"key":"B","text":"노드를 삭제한다."},{"key":"C","text":"트리 높이를 늘린다."},{"key":"D","text":"삽입된 노드를 부모와 비교하며 힙 성질을 회복한다."}]',
     'D', 'sift-up 은 새 노드를 부모와 교환하며 성질 회복.', 3, 'ai_initial',
     60, 'devhash-weakness-9516', 'approved', NULL, 'ai_generated', NULL, 1, 1, 1, 1, 1, NOW(), NOW(), NOW()),
    (9517, 9304, 9404, 'MCQ_4', 'application', 'n개 원소 최대 힙에서 최댓값 삭제의 시간복잡도는?',
     '[{"key":"A","text":"O(log n)"},{"key":"B","text":"O(1)"},{"key":"C","text":"O(n)"},{"key":"D","text":"O(n log n)"}]',
     'A', '루트 삭제 후 sift-down 으로 O(log n).', 3, 'ai_initial',
     60, 'devhash-weakness-9517', 'approved', NULL, 'ai_generated', NULL, 1, 1, 1, 1, 1, NOW(), NOW(), NOW()),
    (9518, 9304, 9404, 'MCQ_4', 'misconception', '힙 연산에 대한 오해로 옳은 것은?',
     '[{"key":"A","text":"삭제는 O(1)이다."},{"key":"B","text":"''임의 원소 탐색이 빠르다''고 오해하기 쉽지만 힙의 탐색은 O(n)이다."},{"key":"C","text":"삽입은 전체 정렬을 유발한다."},{"key":"D","text":"build-heap 은 O(n log n)만 가능하다."}]',
     'B', '힙의 임의 탐색은 O(n).', 3, 'ai_initial',
     60, 'devhash-weakness-9518', 'approved', NULL, 'ai_generated', NULL, 1, 1, 1, 1, 1, NOW(), NOW(), NOW()),
    (9519, 9304, 9404, 'MCQ_4', 'application', '상향식(bottom-up) build-heap 의 시간복잡도로 가장 정확한 것은?',
     '[{"key":"A","text":"O(n log n)"},{"key":"B","text":"O(log n)"},{"key":"C","text":"O(n)"},{"key":"D","text":"O(n^2)"}]',
     'C', 'bottom-up build-heap 은 O(n).', 3, 'ai_initial',
     60, 'devhash-weakness-9519', 'approved', NULL, 'ai_generated', NULL, 1, 1, 1, 1, 1, NOW(), NOW(), NOW()),
    (9520, 9304, 9404, 'MCQ_4', 'comparison', 'sift-up 과 sift-down 의 차이로 옳은 것은?',
     '[{"key":"A","text":"둘은 완전히 동일하다."},{"key":"B","text":"sift-up 은 삭제에만 쓰인다."},{"key":"C","text":"sift-down 은 삽입에만 쓰인다."},{"key":"D","text":"삽입은 주로 sift-up, 루트 삭제는 주로 sift-down 을 사용한다."}]',
     'D', '삽입=sift-up, 루트 삭제=sift-down.', 3, 'ai_initial',
     60, 'devhash-weakness-9520', 'approved', NULL, 'ai_generated', NULL, 1, 1, 1, 1, 1, NOW(), NOW(), NOW());

-- =============================================================================
-- 4) 완료(status=COMPLETED) 모의고사 세션 3개 (user_id=1, 같은 과목 9001)
--    9601: SUBJECT_MOCK      (과목 전체, 10문항, 정답7/오답3, rate 70.00, 총 705초)
--    9602: CATEGORY_LV2_MOCK (이진 트리 9201, 5문항, 정답3/오답2, rate 60.00, 총 430초)
--    9603: CATEGORY_LV2_MOCK (힙 9202,      5문항, 정답3/오답2, rate 60.00, 총 360초)
--   NN: user_id, session_type, title, total_question_count, correct_count, wrong_count,
--       total_time_spent_sec, status, started_at, created_at, updated_at
--   (subject_id/category_lv1_id/category_lv2_id/concept_id 는 nullable FK — 범위에 맞춰 세팅,
--    correct_rate 는 nullable, completed_at 는 완료라 세팅)
-- =============================================================================
INSERT INTO solve_sessions
    (solve_session_id, user_id, session_type, subject_id, category_lv1_id, category_lv2_id, concept_id,
     title, total_question_count, correct_count, wrong_count, correct_rate, total_time_spent_sec,
     status, started_at, completed_at, created_at, updated_at)
VALUES
    (9601, 1, 'SUBJECT_MOCK',      9001, NULL, NULL, NULL,
     '[DEV] 자료구조 과목 모의고사', 10, 7, 3, 70.00, 705,
     'COMPLETED', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY + INTERVAL 705 SECOND,
     NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY),
    (9602, 1, 'CATEGORY_LV2_MOCK', 9001, 9101, 9201, NULL,
     '[DEV] 이진 트리 챕터 모의고사', 5, 3, 2, 60.00, 430,
     'COMPLETED', NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY + INTERVAL 430 SECOND,
     NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY),
    (9603, 1, 'CATEGORY_LV2_MOCK', 9001, 9101, 9202, NULL,
     '[DEV] 힙 챕터 모의고사', 5, 3, 2, 60.00, 360,
     'COMPLETED', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY + INTERVAL 360 SECOND,
     NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY);

-- =============================================================================
-- 5) 문항별 solve_logs (약점 근거가 개념별로 생기도록 WRONG/LONG_TIME/HINT_USED 혼합)
--    unique (solve_session_id, question_id), (solve_session_id, question_order) 위반 없음.
--   NN: solve_session_id, user_id, question_id, question_order, correct_answer_snapshot,
--       is_correct, time_spent_sec, hint_used, hint_count, option_change_count,
--       is_answer_changed, is_bookmarked, solved_at, created_at
--   (selected_answer/expected_solve_sec_snapshot 는 nullable — 판정 재현 위해 모두 채움)
--
--   개념별 근거 누적:
--     9301: WRONG x2, LONG_TIME x2
--     9302: HINT x2,  WRONG x1
--     9303: WRONG x2, HINT x1, LONG_TIME x1
--     9304: WRONG x2, LONG_TIME x2, HINT x1
-- =============================================================================

-- 5-1) 세션 9601 (SUBJECT_MOCK) — 10문항, logs 9701~9710
INSERT INTO solve_logs
    (solve_log_id, solve_session_id, user_id, question_id, question_order, selected_answer,
     correct_answer_snapshot, is_correct, time_spent_sec, expected_solve_sec_snapshot,
     hint_used, hint_count, option_change_count, is_answer_changed, is_bookmarked,
     solved_at, created_at)
VALUES
    -- 9301
    (9701, 9601, 1, 9503, 1, 'A', 'C', 0,  60, 60, 0, 0, 0, 0, 0, NOW() - INTERVAL 1 DAY + INTERVAL 705 SECOND, NOW() - INTERVAL 1 DAY + INTERVAL 705 SECOND), -- WRONG
    (9702, 9601, 1, 9504, 2, 'D', 'D', 1, 120, 60, 0, 0, 0, 0, 0, NOW() - INTERVAL 1 DAY + INTERVAL 705 SECOND, NOW() - INTERVAL 1 DAY + INTERVAL 705 SECOND), -- LONG_TIME (>=120)
    (9703, 9601, 1, 9505, 3, 'A', 'A', 1,  45, 60, 0, 0, 0, 0, 0, NOW() - INTERVAL 1 DAY + INTERVAL 705 SECOND, NOW() - INTERVAL 1 DAY + INTERVAL 705 SECOND), -- 정상 정답
    -- 9302
    (9704, 9601, 1, 9509, 4, 'A', 'A', 1,  90, 90, 1, 2, 1, 0, 0, NOW() - INTERVAL 1 DAY + INTERVAL 705 SECOND, NOW() - INTERVAL 1 DAY + INTERVAL 705 SECOND), -- HINT_USED
    (9705, 9601, 1, 9510, 5, 'B', 'B', 1,  70, 90, 0, 0, 0, 0, 0, NOW() - INTERVAL 1 DAY + INTERVAL 705 SECOND, NOW() - INTERVAL 1 DAY + INTERVAL 705 SECOND), -- 정상 정답
    -- 9303
    (9706, 9601, 1, 9514, 6, 'D', 'B', 0,  45, 45, 0, 0, 0, 0, 0, NOW() - INTERVAL 1 DAY + INTERVAL 705 SECOND, NOW() - INTERVAL 1 DAY + INTERVAL 705 SECOND), -- WRONG
    (9707, 9601, 1, 9515, 7, 'C', 'C', 1,  35, 45, 0, 0, 0, 0, 0, NOW() - INTERVAL 1 DAY + INTERVAL 705 SECOND, NOW() - INTERVAL 1 DAY + INTERVAL 705 SECOND), -- 정상 정답
    -- 9304
    (9708, 9601, 1, 9518, 8, 'B', 'B', 1,  60, 60, 1, 3, 2, 1, 0, NOW() - INTERVAL 1 DAY + INTERVAL 705 SECOND, NOW() - INTERVAL 1 DAY + INTERVAL 705 SECOND), -- HINT_USED
    (9709, 9601, 1, 9519, 9, 'C', 'C', 1, 120, 60, 0, 0, 0, 0, 0, NOW() - INTERVAL 1 DAY + INTERVAL 705 SECOND, NOW() - INTERVAL 1 DAY + INTERVAL 705 SECOND), -- LONG_TIME
    (9710, 9601, 1, 9520,10, 'A', 'D', 0,  60, 60, 0, 0, 0, 0, 0, NOW() - INTERVAL 1 DAY + INTERVAL 705 SECOND, NOW() - INTERVAL 1 DAY + INTERVAL 705 SECOND); -- WRONG

-- 5-2) 세션 9602 (CATEGORY_LV2_MOCK / 이진 트리) — 5문항, logs 9711~9715
INSERT INTO solve_logs
    (solve_log_id, solve_session_id, user_id, question_id, question_order, selected_answer,
     correct_answer_snapshot, is_correct, time_spent_sec, expected_solve_sec_snapshot,
     hint_used, hint_count, option_change_count, is_answer_changed, is_bookmarked,
     solved_at, created_at)
VALUES
    -- 9301
    (9711, 9602, 1, 9501, 1, 'B', 'A', 0,  60, 60, 0, 0, 0, 0, 0, NOW() - INTERVAL 3 DAY + INTERVAL 430 SECOND, NOW() - INTERVAL 3 DAY + INTERVAL 430 SECOND), -- WRONG
    (9712, 9602, 1, 9502, 2, 'B', 'B', 1, 120, 60, 0, 0, 0, 0, 0, NOW() - INTERVAL 3 DAY + INTERVAL 430 SECOND, NOW() - INTERVAL 3 DAY + INTERVAL 430 SECOND), -- LONG_TIME
    -- 9302
    (9713, 9602, 1, 9506, 3, 'B', 'B', 1,  90, 90, 1, 2, 1, 0, 0, NOW() - INTERVAL 3 DAY + INTERVAL 430 SECOND, NOW() - INTERVAL 3 DAY + INTERVAL 430 SECOND), -- HINT_USED
    (9714, 9602, 1, 9507, 4, 'A', 'C', 0,  90, 90, 0, 0, 0, 0, 0, NOW() - INTERVAL 3 DAY + INTERVAL 430 SECOND, NOW() - INTERVAL 3 DAY + INTERVAL 430 SECOND), -- WRONG
    (9715, 9602, 1, 9508, 5, 'D', 'D', 1,  70, 90, 0, 0, 0, 0, 0, NOW() - INTERVAL 3 DAY + INTERVAL 430 SECOND, NOW() - INTERVAL 3 DAY + INTERVAL 430 SECOND); -- 정상 정답

-- 5-3) 세션 9603 (CATEGORY_LV2_MOCK / 힙) — 5문항, logs 9716~9720
INSERT INTO solve_logs
    (solve_log_id, solve_session_id, user_id, question_id, question_order, selected_answer,
     correct_answer_snapshot, is_correct, time_spent_sec, expected_solve_sec_snapshot,
     hint_used, hint_count, option_change_count, is_answer_changed, is_bookmarked,
     solved_at, created_at)
VALUES
    -- 9303
    (9716, 9603, 1, 9511, 1, 'A', 'C', 0,  45, 45, 0, 0, 0, 0, 0, NOW() - INTERVAL 2 DAY + INTERVAL 360 SECOND, NOW() - INTERVAL 2 DAY + INTERVAL 360 SECOND), -- WRONG
    (9717, 9603, 1, 9512, 2, 'D', 'D', 1,  45, 45, 1, 1, 0, 0, 0, NOW() - INTERVAL 2 DAY + INTERVAL 360 SECOND, NOW() - INTERVAL 2 DAY + INTERVAL 360 SECOND), -- HINT_USED
    (9718, 9603, 1, 9513, 3, 'A', 'A', 1,  90, 45, 0, 0, 0, 0, 0, NOW() - INTERVAL 2 DAY + INTERVAL 360 SECOND, NOW() - INTERVAL 2 DAY + INTERVAL 360 SECOND), -- LONG_TIME
    -- 9304
    (9719, 9603, 1, 9516, 4, 'A', 'D', 0,  60, 60, 0, 0, 0, 0, 0, NOW() - INTERVAL 2 DAY + INTERVAL 360 SECOND, NOW() - INTERVAL 2 DAY + INTERVAL 360 SECOND), -- WRONG
    (9720, 9603, 1, 9517, 5, 'A', 'A', 1, 120, 60, 0, 0, 0, 0, 0, NOW() - INTERVAL 2 DAY + INTERVAL 360 SECOND, NOW() - INTERVAL 2 DAY + INTERVAL 360 SECOND); -- LONG_TIME

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 6) 검증용 요약 조회 (실행 시 결과 확인용 — 없어도 무방)
-- =============================================================================
SELECT '유저/재화' AS section, u.user_id, u.nickname, u.status, c.coin
  FROM users u LEFT JOIN user_currency c ON c.user_id = u.user_id
 WHERE u.user_id = 1;

SELECT '세션 요약' AS section, s.solve_session_id, s.session_type, s.total_question_count,
       s.correct_count, s.wrong_count, s.correct_rate, s.total_time_spent_sec, s.status
  FROM solve_sessions s
 WHERE s.solve_session_id BETWEEN 9601 AND 9603
 ORDER BY s.solve_session_id;

-- 개념별 근거 집계: WRONG / LONG_TIME(>=expected*2) / HINT_USED 개수
SELECT '개념별 근거' AS section,
       q.concept_id,
       cp.concept_name,
       SUM(CASE WHEN l.is_correct = 0 THEN 1 ELSE 0 END) AS wrong_cnt,
       SUM(CASE WHEN l.is_correct = 1
                 AND l.expected_solve_sec_snapshot IS NOT NULL
                 AND l.time_spent_sec >= l.expected_solve_sec_snapshot * 2
                THEN 1 ELSE 0 END) AS long_time_cnt,
       SUM(CASE WHEN l.hint_used = 1 AND l.hint_count >= 1 THEN 1 ELSE 0 END) AS hint_cnt,
       COUNT(*) AS total_logs
  FROM solve_logs l
  JOIN question q  ON q.question_id = l.question_id
  JOIN concept cp  ON cp.concept_id = q.concept_id
 WHERE l.solve_session_id BETWEEN 9601 AND 9603
 GROUP BY q.concept_id, cp.concept_name
 ORDER BY q.concept_id;
