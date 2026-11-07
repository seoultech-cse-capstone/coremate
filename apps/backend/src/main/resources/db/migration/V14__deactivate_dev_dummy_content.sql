-- =============================================================================
-- V14__deactivate_dev_dummy_content.sql
--
-- 운영 DB 에 섞여 들어간 약점 분석 손검증용 더미 콘텐츠를 끈다(#412).
--
-- scripts/db/weakness_dummy_seed.sql 이 심는 과목 [DEV] 자료구조(약점분석)(DEV_DS_WEAKNESS) 아래의
-- 대분류 1개·중분류 2개·개념 4개·오개념·문항 20개가 2026-09-28 운영 첫 적재 때 로컬 DB 콘텐츠와 함께 들어갔다.
-- 과목만 꺼져 있어 문제집 화면에는 안 보이지만, 나머지는 켜져 있어서
--   - 복습 화면 「총 개념」(concept.is_active 만 센다)이 469 가 아니라 473 으로 나오고, 이 4개는 풀 길이 없어
--     진도가 100% 가 될 수 없다.
--   - 오늘의 퀴즈 후보(question.is_active·is_daily_quiz_eligible 만 본다)에 이 20문항이 들어 있다.
--
-- 행은 지우지 않고 끈다. 풀이·퀴즈 기록이 문항을 참조할 수 있고, 되돌리기도 쉽다.
-- 더미가 없는 DB(새로 만든 로컬·테스트)에서는 0행 갱신으로 끝난다. 데이터만 바꾸므로 한 트랜잭션으로 적용된다.
-- 로컬에서 더미가 다시 필요하면 weakness_dummy_seed.sql 을 다시 돌린다(지우고 켠 채로 다시 넣는다).
-- =============================================================================

UPDATE question q
    JOIN concept c ON c.concept_id = q.concept_id
SET q.is_active  = 0,
    q.updated_at = NOW()
WHERE c.concept_code IN ('DEV_CPT_COMPLETE_BINTREE', 'DEV_CPT_FULL_BINTREE', 'DEV_CPT_HEAP_PROP', 'DEV_CPT_HEAP_OPS')
  AND q.is_active = 1;

UPDATE common_mistake m
    JOIN concept c ON c.concept_id = m.concept_id
SET m.is_active  = 0,
    m.updated_at = NOW()
WHERE c.concept_code IN ('DEV_CPT_COMPLETE_BINTREE', 'DEV_CPT_FULL_BINTREE', 'DEV_CPT_HEAP_PROP', 'DEV_CPT_HEAP_OPS')
  AND m.is_active = 1;

UPDATE concept
SET is_active  = 0,
    updated_at = NOW()
WHERE concept_code IN ('DEV_CPT_COMPLETE_BINTREE', 'DEV_CPT_FULL_BINTREE', 'DEV_CPT_HEAP_PROP', 'DEV_CPT_HEAP_OPS')
  AND is_active = 1;

UPDATE category_lv2
SET is_active  = 0,
    updated_at = NOW()
WHERE category_lv2_code IN ('DEV_DS_BINTREE', 'DEV_DS_HEAP')
  AND is_active = 1;

UPDATE category_lv1
SET is_active  = 0,
    updated_at = NOW()
WHERE category_lv1_code = 'DEV_DS_TREE'
  AND is_active = 1;
