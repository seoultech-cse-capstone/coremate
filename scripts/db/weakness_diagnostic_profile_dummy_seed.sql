-- =============================================================================
-- weakness_diagnostic_profile_dummy_seed.sql
--
-- 목적: 사용자 약점 분석표 기능(feat/weakness-analysis) 손검증용 진단 프로필 더미.
--       OpenAI 비용 없이(전부 손작성 SQL) concept_diagnostic_profile /
--       question_diagnostic_profile 두 테이블을, weakness_dummy_seed.sql 이 심는
--       더미 개념(9301~9304)·문항(9501~9520)에 맞춰 채운다. (dev 전용)
--
-- !! 선행 조건 !!
--   1) 백엔드를 한 번 기동해 진단 프로필 테이블이 생성돼 있어야 한다(Flyway 마이그레이션).
--   2) weakness_dummy_seed.sql 을 먼저 실행해 concept 9301~9304 / question 9501~9520
--      행이 존재해야 한다(FK 대상). 이 스크립트는 그 고정 ID 를 참조만 한다.
--
-- 실행:
--   mysql -u coremate -p coremate < scripts/db/weakness_diagnostic_profile_dummy_seed.sql
--
-- 재실행(idempotent): 대상 범위를 DELETE 후 재INSERT 한다.
--
-- 참고: review_status='APPROVED', diagnostic_profile_version='v1'.
--       TEXT 컬럼 중 목록/구조형(key_points, confusion_comparisons,
--       prerequisite_concepts, basic_review_points, answer_misconception_map)은
--       JSON 문자열로 저장한다(백엔드 소비 시 파싱). 작은따옴표는 SQL 이스케이프를
--       피하려 본문에서 사용하지 않는다.
-- =============================================================================

SET NAMES utf8mb4;

DELETE FROM question_diagnostic_profile WHERE question_id BETWEEN 9501 AND 9520;
DELETE FROM concept_diagnostic_profile  WHERE concept_id  BETWEEN 9301 AND 9304;

-- =============================================================================
-- 1) 개념별 진단 프로필 (concept_diagnostic_profile) — 개념 9301~9304
-- =============================================================================
INSERT INTO concept_diagnostic_profile
    (concept_id, concept_core, concept_explanation, key_points, confusion_comparisons,
     prerequisite_concepts, learning_example, misconception_notes, basic_review_points,
     misconception_mapping_coverage, diagnostic_profile_version, review_status,
     created_at, updated_at)
VALUES
    (9301,
     '완전 이진 트리는 마지막 레벨을 제외한 모든 레벨이 꽉 차 있고, 마지막 레벨은 왼쪽부터 채워진 이진 트리다. 배열 표현이 자연스러워 힙 구현의 기반이 된다.',
     '완전 이진 트리는 노드를 위에서 아래로, 각 레벨에서는 왼쪽에서 오른쪽으로 빈틈없이 채운 형태다. 1기반 배열로 표현하면 인덱스 i의 자식이 2i, 2i+1 로 계산돼 포인터 없이도 부모-자식 접근이 쉽다. 이 성질이 힙을 배열로 구현할 수 있게 해준다.',
     '["마지막 레벨을 제외한 모든 레벨이 꽉 참","마지막 레벨은 왼쪽부터 채움","배열 인덱스로 부모-자식 계산이 쉬움"]',
     '[{"compared_with":"포화 이진 트리","difference":"포화는 모든 리프가 같은 레벨인 특수한 경우이고, 완전은 마지막 레벨이 왼쪽부터 부분적으로 채워져도 된다."}]',
     '["이진 트리"]',
     '노드가 6개인 완전 이진 트리를 1기반 배열로 표현하면 마지막 내부 노드 인덱스는 floor(6/2)=3 이다.',
     '모든 레벨이 완전히 채워져야 한다고 오해하기 쉽다. 마지막 레벨은 왼쪽부터 부분적으로 채워도 완전 이진 트리다.',
     '["이진 트리의 레벨과 높이 개념","배열의 1기반 인덱싱"]',
     1.000, 'v1', 'APPROVED', NOW(), NOW()),

    (9302,
     '포화 이진 트리는 모든 내부 노드가 두 자식을 갖고 모든 리프가 같은 레벨에 있는 이진 트리다. 높이 h에서 노드 수가 2^(h+1)-1 로 결정된다.',
     '포화 이진 트리는 빈 자리 없이 가장 빽빽하게 채워진 트리다. 모든 리프가 같은 깊이에 있으므로 높이가 h 이면 노드 수는 정확히 2^(h+1)-1 개다. 포화는 완전 이진 트리의 특수한 경우이지만 그 역은 성립하지 않는다.',
     '["모든 리프가 같은 레벨","완전 이진 트리의 특수 경우","노드 수 2^(h+1)-1"]',
     '[{"compared_with":"완전 이진 트리","difference":"완전은 마지막 레벨이 왼쪽부터 부분적으로 채워져도 되지만, 포화는 모든 리프가 같은 레벨로 꽉 차 있어야 한다."}]',
     '["이진 트리","완전 이진 트리"]',
     '높이가 2인 포화 이진 트리의 노드 수는 2^3-1=7 개다.',
     '포화와 완전을 같은 것으로 보거나, 완전이면 곧 포화라고 혼동하기 쉽다. 포화는 완전의 특수한 경우일 뿐이다.',
     '["트리 높이와 노드 수 관계","완전 이진 트리 정의"]',
     1.000, 'v1', 'APPROVED', NOW(), NOW()),

    (9303,
     '힙은 부모-자식 간 부분 순서를 보장하는 완전 이진 트리 기반 구조다. 최대 힙은 부모가 자식보다 크거나 같으며, 전체 정렬이 아니라 부분 순서만 보장한다.',
     '힙은 완전 이진 트리 위에 부모-자식 대소 관계 조건을 얹은 자료구조다. 최대 힙에서는 모든 부모가 자식 이상이므로 루트가 최댓값이 된다. 형제 간 순서나 전체 배열 정렬은 보장하지 않기 때문에 최댓값 접근은 빠르지만 임의 값 탐색은 느리다.',
     '["최대 힙은 부모>=자식","형제 간 순서는 미보장","루트가 최댓값 또는 최솟값"]',
     '[{"compared_with":"이진 탐색 트리","difference":"BST는 중위 순회가 정렬되지만, 힙은 부분 순서만 보장해 형제 간 순서가 없다."}]',
     '["완전 이진 트리"]',
     '배열 [9,7,8,3,6] 은 모든 부모가 자식 이상이므로 유효한 최대 힙이다.',
     '힙이 배열 전체가 정렬된 구조라고 오해하기 쉽다. 힙은 부모-자식 간 부분 순서만 보장한다.',
     '["완전 이진 트리의 배열 표현","부분 순서와 전체 정렬의 차이"]',
     1.000, 'v1', 'APPROVED', NOW(), NOW()),

    (9304,
     '힙 연산은 삽입(sift-up), 삭제(sift-down), build-heap 등 힙 성질을 유지하는 연산이다. 삽입과 삭제는 O(log n), build-heap 은 O(n) 이다.',
     '힙에 값을 삽입할 때는 마지막 자리에 넣고 부모와 비교하며 위로 올리는 sift-up 으로 성질을 회복한다. 루트를 삭제할 때는 마지막 원소를 루트로 옮긴 뒤 자식과 비교하며 내리는 sift-down 을 쓴다. 두 연산 모두 트리 높이에 비례해 O(log n) 이고, 상향식 build-heap 은 O(n) 에 힙을 만든다.',
     '["삽입은 sift-up 으로 성질 회복","루트 삭제는 sift-down","삽입 삭제는 O(log n), build-heap 은 O(n)"]',
     '[{"compared_with":"힙의 성질","difference":"힙의 성질은 구조가 지켜야 할 조건이고, 힙 연산은 그 조건을 유지하도록 값을 옮기는 절차다."}]',
     '["힙의 성질","완전 이진 트리"]',
     'n개 원소 최대 힙에서 최댓값 삭제는 루트를 제거하고 sift-down 하므로 O(log n) 이다.',
     '힙에서 임의 원소 탐색이 빠르다고 오해하기 쉽다. 힙은 최댓값 또는 최솟값 접근만 O(1) 이고 임의 탐색은 O(n) 이며, 삽입 삭제가 O(log n) 이다.',
     '["로그 시간 복잡도의 의미","sift-up 과 sift-down 의 차이"]',
     1.000, 'v1', 'APPROVED', NOW(), NOW());

-- =============================================================================
-- 2) 문항별 진단 프로필 (question_diagnostic_profile) — 문항 9501~9520 전체
--    어떤 회차 조합을 선택하든 대표 근거로 뽑힌 문항에 프로필이 붙도록 20문항을 모두 채운다.
--    (일부만 채우면 그 문항이 근거로 뽑혔을 때 skill_type/오개념 코드가 비어 AI 입력이 빈약해진다)
--    expected_solve_sec/difficulty 는 weakness_dummy_seed.sql 의 문항 값과 일치시킨다.
--
--    misconception_code 규칙(파이프라인 schemas/question_diagnostic_profile_schema.py 와 동일):
--      정답 선택지            = NONE
--      카탈로그에 없는 오개념 = OTHER
--      개념별 공통 오개념     = {concept_code}_M01~_M03
--                              (weakness_dummy_seed.sql 의 common_mistake 9401~9412 에 대응)
--    NONE/OTHER 는 백엔드가 반복 오개념 집계에서 제외한다.
-- =============================================================================
INSERT INTO question_diagnostic_profile
    (question_id, concept_id, diagnostic_summary, question_skill_type,
     expected_solve_sec, difficulty, answer_misconception_map,
     diagnostic_profile_version, review_status, created_at, updated_at)
VALUES
    -- ---------------- 개념 9301 완전 이진 트리 ----------------
    (9501, 9301,
     '완전 이진 트리의 정의를 정확히 아는지 진단한다. 마지막 레벨은 왼쪽부터 부분적으로 채워도 된다는 조건이 핵심이다.',
     'RECALL', 60, 3,
     '[{"option_key":"A","is_correct":true,"interpretation":"마지막 레벨을 제외한 모든 레벨이 꽉 차고 마지막은 왼쪽부터 채운다는 정의를 정확히 이해했다.","misconception_code":"NONE"},{"option_key":"B","is_correct":false,"interpretation":"모든 레벨이 예외 없이 꽉 차야 한다고 보아 완전 이진 트리를 포화 이진 트리로 오해한 경우다.","misconception_code":"DEV_CPT_COMPLETE_BINTREE_M01"},{"option_key":"C","is_correct":false,"interpretation":"모든 노드의 차수가 2여야 한다고 보아 완전 이진 트리에 더 강한 조건을 붙인 경우다.","misconception_code":"DEV_CPT_COMPLETE_BINTREE_M01"},{"option_key":"D","is_correct":false,"interpretation":"좌우 서브트리 높이 조건을 완전 이진 트리의 정의로 착각한 경우다.","misconception_code":"OTHER"}]',
     'v1', 'APPROVED', NOW(), NOW()),

    (9502, 9301,
     '완전 이진 트리와 포화 이진 트리의 차이를 구분할 수 있는지 진단한다.',
     'ANALYSIS', 60, 3,
     '[{"option_key":"A","is_correct":false,"interpretation":"두 개념을 동일하게 보아 완전과 포화를 구분하지 못한 경우다.","misconception_code":"DEV_CPT_COMPLETE_BINTREE_M01"},{"option_key":"B","is_correct":true,"interpretation":"포화는 모든 레벨이 꽉 차고 완전은 마지막 레벨만 왼쪽부터 채워도 된다는 차이를 정확히 이해했다.","misconception_code":"NONE"},{"option_key":"C","is_correct":false,"interpretation":"두 구조의 노드 수 대소를 잘못 일반화한 경우다.","misconception_code":"OTHER"},{"option_key":"D","is_correct":false,"interpretation":"포화 이진 트리의 노드 수 조건을 홀짝으로 잘못 이해한 경우다.","misconception_code":"OTHER"}]',
     'v1', 'APPROVED', NOW(), NOW()),

    (9503, 9301,
     '완전 이진 트리의 배열 표현에서 마지막 내부 노드 인덱스를 계산할 수 있는지 진단한다. floor(n/2) 규칙을 적용해야 한다.',
     'APPLICATION', 60, 3,
     '[{"option_key":"A","is_correct":false,"interpretation":"마지막 내부 노드를 루트로 보아 배열 인덱스 계산 규칙을 적용하지 못한 경우다.","misconception_code":"DEV_CPT_COMPLETE_BINTREE_M02"},{"option_key":"B","is_correct":false,"interpretation":"floor(n/2) 계산을 잘못해 인덱스를 하나 작게 잡은 경우다.","misconception_code":"DEV_CPT_COMPLETE_BINTREE_M02"},{"option_key":"C","is_correct":true,"interpretation":"n=6 에서 floor(6/2)=3 이라는 계산을 정확히 수행했다.","misconception_code":"NONE"},{"option_key":"D","is_correct":false,"interpretation":"마지막 노드(리프)를 마지막 내부 노드로 혼동한 경우다.","misconception_code":"DEV_CPT_COMPLETE_BINTREE_M02"}]',
     'v1', 'APPROVED', NOW(), NOW()),

    (9504, 9301,
     '완전 이진 트리의 정의를 포화 이진 트리와 구분해 이해했는지 진단한다. 마지막 레벨이 왼쪽부터 부분적으로 채워져도 됨을 알아야 맞출 수 있다.',
     'COMPREHENSION', 60, 3,
     '[{"option_key":"A","is_correct":false,"interpretation":"트리 높이를 노드 수와 무관하게 고정값으로 오해한 경우다.","misconception_code":"OTHER"},{"option_key":"B","is_correct":false,"interpretation":"완전 이진 트리의 리프 구조를 잘못 이해한 경우다.","misconception_code":"OTHER"},{"option_key":"C","is_correct":false,"interpretation":"구조 조건과 값 정렬을 혼동해 정렬을 조건으로 착각한 경우다.","misconception_code":"DEV_CPT_COMPLETE_BINTREE_M03"},{"option_key":"D","is_correct":true,"interpretation":"마지막 레벨은 왼쪽부터 부분적으로 채워도 된다는 정의를 정확히 이해했다.","misconception_code":"NONE"}]',
     'v1', 'APPROVED', NOW(), NOW()),

    (9505, 9301,
     '완전 이진 트리의 예를 판별할 수 있는지 진단한다.',
     'APPLICATION', 60, 3,
     '[{"option_key":"A","is_correct":true,"interpretation":"마지막 레벨이 왼쪽부터 빈틈없이 채워진 형태가 완전 이진 트리임을 정확히 판별했다.","misconception_code":"NONE"},{"option_key":"B","is_correct":false,"interpretation":"편향 트리를 완전 이진 트리로 보아 레벨 채움 조건을 확인하지 못한 경우다.","misconception_code":"OTHER"},{"option_key":"C","is_correct":false,"interpretation":"마지막 레벨 중간에 빈 자리가 있어도 된다고 보아 왼쪽부터 채움 조건을 놓친 경우다.","misconception_code":"DEV_CPT_COMPLETE_BINTREE_M01"},{"option_key":"D","is_correct":false,"interpretation":"이진 트리의 자식 수 조건 자체를 잘못 이해한 경우다.","misconception_code":"OTHER"}]',
     'v1', 'APPROVED', NOW(), NOW()),

    -- ---------------- 개념 9302 포화 이진 트리 ----------------
    (9506, 9302,
     '포화 이진 트리의 정의를 정확히 아는지 진단한다. 모든 리프가 같은 레벨에 있어야 한다.',
     'RECALL', 90, 4,
     '[{"option_key":"A","is_correct":false,"interpretation":"마지막 레벨만 채워지면 된다고 보아 포화를 완전 이진 트리와 혼동한 경우다.","misconception_code":"DEV_CPT_FULL_BINTREE_M01"},{"option_key":"B","is_correct":true,"interpretation":"모든 내부 노드가 두 자식을 갖고 모든 리프가 같은 레벨에 있다는 정의를 정확히 이해했다.","misconception_code":"NONE"},{"option_key":"C","is_correct":false,"interpretation":"노드가 하나뿐인 경우만 포화로 보아 정의를 지나치게 좁게 이해한 경우다.","misconception_code":"OTHER"},{"option_key":"D","is_correct":false,"interpretation":"이진 트리의 차수 조건 자체를 잘못 이해한 경우다.","misconception_code":"OTHER"}]',
     'v1', 'APPROVED', NOW(), NOW()),

    (9507, 9302,
     '높이 h 포화 이진 트리의 노드 수 공식을 적용할 수 있는지 진단한다.',
     'APPLICATION', 90, 4,
     '[{"option_key":"A","is_correct":false,"interpretation":"노드 수를 높이와 같다고 보아 공식을 적용하지 못한 경우다.","misconception_code":"DEV_CPT_FULL_BINTREE_M02"},{"option_key":"B","is_correct":false,"interpretation":"노드 수를 높이의 상수배로 보아 지수 증가를 놓친 경우다.","misconception_code":"DEV_CPT_FULL_BINTREE_M02"},{"option_key":"C","is_correct":true,"interpretation":"2^(h+1)-1 공식을 정확히 적용했다.","misconception_code":"NONE"},{"option_key":"D","is_correct":false,"interpretation":"노드 수를 제곱 관계로 보아 지수 증가를 잘못 이해한 경우다.","misconception_code":"DEV_CPT_FULL_BINTREE_M02"}]',
     'v1', 'APPROVED', NOW(), NOW()),

    (9508, 9302,
     '포화 이진 트리가 완전 이진 트리에 포함되는 관계를 이해했는지 진단한다.',
     'ANALYSIS', 90, 4,
     '[{"option_key":"A","is_correct":false,"interpretation":"두 구조를 배타적으로 보아 포함 관계를 반대로 이해한 경우다.","misconception_code":"DEV_CPT_FULL_BINTREE_M01"},{"option_key":"B","is_correct":false,"interpretation":"포함 관계를 판단할 수 없다고 보아 정의 비교를 하지 못한 경우다.","misconception_code":"OTHER"},{"option_key":"C","is_correct":false,"interpretation":"포화면 완전이 될 수 없다고 보아 포함 관계를 반대로 이해한 경우다.","misconception_code":"DEV_CPT_FULL_BINTREE_M01"},{"option_key":"D","is_correct":true,"interpretation":"모든 포화 이진 트리가 완전 이진 트리이기도 하다는 포함 관계를 정확히 이해했다.","misconception_code":"NONE"}]',
     'v1', 'APPROVED', NOW(), NOW()),

    (9509, 9302,
     '완전과 포화 이진 트리의 포함 관계를 바르게 이해했는지 진단한다. 완전이 곧 포화는 아니라는 점이 핵심이다.',
     'COMPREHENSION', 90, 4,
     '[{"option_key":"A","is_correct":true,"interpretation":"완전이면 곧 포화라는 착각을 정확히 교정해 포함 관계를 바르게 이해했다.","misconception_code":"NONE"},{"option_key":"B","is_correct":false,"interpretation":"포화 이진 트리에 리프가 없다고 보아 리프 구조를 오해한 경우다.","misconception_code":"DEV_CPT_FULL_BINTREE_M03"},{"option_key":"C","is_correct":false,"interpretation":"포화의 노드 수 규칙을 짝수 여부로 잘못 일반화한 경우다.","misconception_code":"DEV_CPT_FULL_BINTREE_M02"},{"option_key":"D","is_correct":false,"interpretation":"포화의 높이 개념을 0으로 오해한 경우다.","misconception_code":"OTHER"}]',
     'v1', 'APPROVED', NOW(), NOW()),

    (9510, 9302,
     '포화 이진 트리의 예를 판별할 수 있는지 진단한다.',
     'APPLICATION', 90, 4,
     '[{"option_key":"A","is_correct":false,"interpretation":"마지막 레벨에 빈 자리가 있어도 포화로 보아 완전과 혼동한 경우다.","misconception_code":"DEV_CPT_FULL_BINTREE_M01"},{"option_key":"B","is_correct":true,"interpretation":"높이 2에서 노드가 7개로 꽉 찬 형태가 포화임을 정확히 판별했다.","misconception_code":"NONE"},{"option_key":"C","is_correct":false,"interpretation":"편향 트리를 포화로 보아 리프 레벨 조건을 확인하지 못한 경우다.","misconception_code":"DEV_CPT_FULL_BINTREE_M03"},{"option_key":"D","is_correct":false,"interpretation":"노드 수가 공식에 맞지 않는데도 포화로 판단한 경우다.","misconception_code":"DEV_CPT_FULL_BINTREE_M02"}]',
     'v1', 'APPROVED', NOW(), NOW()),

    -- ---------------- 개념 9303 힙의 성질 ----------------
    (9511, 9303,
     '최대 힙의 힙 성질(부모>=자식)을 정확히 아는지 진단한다. 부분 순서와 전체 정렬을 구분해야 한다.',
     'RECALL', 45, 2,
     '[{"option_key":"A","is_correct":false,"interpretation":"좌-루트-우 대소 관계를 적용해 힙을 이진 탐색 트리로 혼동한 경우다.","misconception_code":"DEV_CPT_HEAP_PROP_M02"},{"option_key":"B","is_correct":false,"interpretation":"리프 값이 모두 같아야 한다고 오해한 경우다.","misconception_code":"OTHER"},{"option_key":"C","is_correct":true,"interpretation":"모든 부모가 자식 이상이라는 최대 힙의 부분 순서를 정확히 이해했다.","misconception_code":"NONE"},{"option_key":"D","is_correct":false,"interpretation":"힙을 전체가 정렬된 배열로 오해한 경우다.","misconception_code":"DEV_CPT_HEAP_PROP_M01"}]',
     'v1', 'APPROVED', NOW(), NOW()),

    (9512, 9303,
     '힙이 전체 정렬 구조라는 오해를 교정했는지 진단한다. 힙은 부분 순서만 보장한다는 점을 알아야 한다.',
     'COMPREHENSION', 45, 2,
     '[{"option_key":"A","is_correct":false,"interpretation":"힙이 항상 정렬되어 있다고 보아 전체 정렬 구조로 오해한 경우다.","misconception_code":"DEV_CPT_HEAP_PROP_M01"},{"option_key":"B","is_correct":false,"interpretation":"힙을 이진 탐색 트리로 보아 두 구조를 혼동한 경우다.","misconception_code":"DEV_CPT_HEAP_PROP_M02"},{"option_key":"C","is_correct":false,"interpretation":"루트가 최소이자 최대라고 잘못 이해한 경우다.","misconception_code":"OTHER"},{"option_key":"D","is_correct":true,"interpretation":"힙이 부분 순서만 보장한다는 성질을 정확히 이해했다.","misconception_code":"NONE"}]',
     'v1', 'APPROVED', NOW(), NOW()),

    (9513, 9303,
     '힙과 이진 탐색 트리의 순서 보장 차이를 구분할 수 있는지 진단한다.',
     'ANALYSIS', 45, 2,
     '[{"option_key":"A","is_correct":true,"interpretation":"BST 는 중위 순회가 정렬되고 힙은 형제 간 순서를 보장하지 않는다는 차이를 정확히 이해했다.","misconception_code":"NONE"},{"option_key":"B","is_correct":false,"interpretation":"힙과 BST 를 같은 자료구조로 보아 두 구조를 혼동한 경우다.","misconception_code":"DEV_CPT_HEAP_PROP_M02"},{"option_key":"C","is_correct":false,"interpretation":"힙의 임의 탐색이 항상 O(log n) 이라고 오해한 경우다.","misconception_code":"OTHER"},{"option_key":"D","is_correct":false,"interpretation":"BST 가 항상 완전 이진 트리라고 오해한 경우다.","misconception_code":"OTHER"}]',
     'v1', 'APPROVED', NOW(), NOW()),

    (9514, 9303,
     '1-기반 배열 힙에서 자식 인덱스 계산 규칙(2i)을 적용할 수 있는지 진단한다.',
     'APPLICATION', 45, 2,
     '[{"option_key":"A","is_correct":false,"interpretation":"인덱스를 하나 줄이는 방식으로 보아 자식 계산 규칙을 잘못 적용한 경우다.","misconception_code":"DEV_CPT_HEAP_PROP_M03"},{"option_key":"B","is_correct":true,"interpretation":"1-기반 배열에서 왼쪽 자식이 2i 라는 규칙을 정확히 적용했다.","misconception_code":"NONE"},{"option_key":"C","is_correct":false,"interpretation":"부모 인덱스 계산식(floor(i/2))과 자식 계산식을 뒤바꾼 경우다.","misconception_code":"DEV_CPT_HEAP_PROP_M03"},{"option_key":"D","is_correct":false,"interpretation":"0-기반 배열의 오른쪽 자식 공식을 1-기반에 잘못 적용한 경우다.","misconception_code":"DEV_CPT_HEAP_PROP_M03"}]',
     'v1', 'APPROVED', NOW(), NOW()),

    (9515, 9303,
     '유효한 최대 힙 배열을 판별할 수 있는지 진단한다.',
     'APPLICATION', 45, 2,
     '[{"option_key":"A","is_correct":false,"interpretation":"오름차순 배열을 최대 힙으로 보아 정렬과 힙 성질을 혼동한 경우다.","misconception_code":"DEV_CPT_HEAP_PROP_M01"},{"option_key":"B","is_correct":false,"interpretation":"부모가 자식보다 작은 배치를 최대 힙으로 판단한 경우다.","misconception_code":"OTHER"},{"option_key":"C","is_correct":true,"interpretation":"모든 부모가 자식 이상인 배열을 정확히 판별했다.","misconception_code":"NONE"},{"option_key":"D","is_correct":false,"interpretation":"루트가 최댓값이 아닌 배열을 최대 힙으로 판단한 경우다.","misconception_code":"OTHER"}]',
     'v1', 'APPROVED', NOW(), NOW()),

    -- ---------------- 개념 9304 힙 연산 ----------------
    (9516, 9304,
     'sift-up 이 힙 성질을 회복하는 절차임을 이해했는지 진단한다.',
     'COMPREHENSION', 60, 3,
     '[{"option_key":"A","is_correct":false,"interpretation":"sift-up 이 트리 전체를 정렬한다고 보아 힙 연산을 정렬 연산으로 오해한 경우다.","misconception_code":"DEV_CPT_HEAP_OPS_M02"},{"option_key":"B","is_correct":false,"interpretation":"sift-up 을 삭제 연산으로 보아 두 연산의 쓰임을 혼동한 경우다.","misconception_code":"DEV_CPT_HEAP_OPS_M03"},{"option_key":"C","is_correct":false,"interpretation":"sift-up 의 목적을 트리 높이 조절로 오해한 경우다.","misconception_code":"OTHER"},{"option_key":"D","is_correct":true,"interpretation":"삽입 노드를 부모와 비교하며 힙 성질을 회복한다는 절차를 정확히 이해했다.","misconception_code":"NONE"}]',
     'v1', 'APPROVED', NOW(), NOW()),

    (9517, 9304,
     '최댓값 삭제가 sift-down 으로 O(log n) 임을 아는지 진단한다.',
     'APPLICATION', 60, 3,
     '[{"option_key":"A","is_correct":true,"interpretation":"루트 삭제 후 sift-down 으로 O(log n) 이라는 점을 정확히 이해했다.","misconception_code":"NONE"},{"option_key":"B","is_correct":false,"interpretation":"최댓값 접근 비용(O(1))과 삭제 비용을 혼동한 경우다.","misconception_code":"OTHER"},{"option_key":"C","is_correct":false,"interpretation":"삭제에 선형 탐색이 필요하다고 보아 힙의 탐색 비용과 삭제 비용을 혼동한 경우다.","misconception_code":"DEV_CPT_HEAP_OPS_M01"},{"option_key":"D","is_correct":false,"interpretation":"단일 삭제 비용을 전체 정렬 비용으로 보아 연산을 정렬로 오해한 경우다.","misconception_code":"DEV_CPT_HEAP_OPS_M02"}]',
     'v1', 'APPROVED', NOW(), NOW()),

    (9518, 9304,
     '힙 연산의 시간복잡도, 특히 임의 탐색이 느리다는 점을 이해했는지 진단한다.',
     'COMPREHENSION', 60, 3,
     '[{"option_key":"A","is_correct":false,"interpretation":"루트 삭제 복잡도를 O(1) 로 오해한 경우다.","misconception_code":"OTHER"},{"option_key":"B","is_correct":true,"interpretation":"힙의 임의 원소 탐색이 O(n) 이라는 점을 정확히 이해했다.","misconception_code":"NONE"},{"option_key":"C","is_correct":false,"interpretation":"삽입이 전체 정렬을 유발한다고 보아 힙 연산을 정렬로 오해한 경우다.","misconception_code":"DEV_CPT_HEAP_OPS_M02"},{"option_key":"D","is_correct":false,"interpretation":"build-heap 을 O(n log n) 으로만 가능하다고 오해한 경우다.","misconception_code":"OTHER"}]',
     'v1', 'APPROVED', NOW(), NOW()),

    (9519, 9304,
     '상향식 build-heap 이 O(n) 임을 아는지 진단한다.',
     'APPLICATION', 60, 3,
     '[{"option_key":"A","is_correct":false,"interpretation":"개별 삽입을 n 번 반복하는 방식으로만 생각해 O(n log n) 으로 판단한 경우다.","misconception_code":"DEV_CPT_HEAP_OPS_M02"},{"option_key":"B","is_correct":false,"interpretation":"build-heap 을 단일 연산 비용으로 보아 전체 원소 처리를 놓친 경우다.","misconception_code":"OTHER"},{"option_key":"C","is_correct":true,"interpretation":"상향식 build-heap 이 O(n) 이라는 점을 정확히 이해했다.","misconception_code":"NONE"},{"option_key":"D","is_correct":false,"interpretation":"힙 구성 비용을 제곱 규모로 과대평가한 경우다.","misconception_code":"OTHER"}]',
     'v1', 'APPROVED', NOW(), NOW()),

    (9520, 9304,
     '삽입과 루트 삭제에 각각 어떤 연산이 쓰이는지 구분할 수 있는지 진단한다.',
     'ANALYSIS', 60, 3,
     '[{"option_key":"A","is_correct":false,"interpretation":"sift-up 과 sift-down 을 같은 연산으로 보아 방향 차이를 구분하지 못한 경우다.","misconception_code":"DEV_CPT_HEAP_OPS_M03"},{"option_key":"B","is_correct":false,"interpretation":"sift-up 의 쓰임을 삭제로 잘못 연결한 경우다.","misconception_code":"DEV_CPT_HEAP_OPS_M03"},{"option_key":"C","is_correct":false,"interpretation":"sift-down 의 쓰임을 삽입으로 잘못 연결한 경우다.","misconception_code":"DEV_CPT_HEAP_OPS_M03"},{"option_key":"D","is_correct":true,"interpretation":"삽입은 sift-up, 루트 삭제는 sift-down 이라는 대응을 정확히 이해했다.","misconception_code":"NONE"}]',
     'v1', 'APPROVED', NOW(), NOW());

-- =============================================================================
-- 3) 검증용 조회
-- =============================================================================
SELECT '개념 프로필' AS section, concept_id, LEFT(concept_core, 24) AS core,
       misconception_mapping_coverage AS cov, review_status
  FROM concept_diagnostic_profile WHERE concept_id BETWEEN 9301 AND 9304 ORDER BY concept_id;

SELECT '문항 프로필' AS section, question_id, concept_id, question_skill_type,
       expected_solve_sec, difficulty, review_status
  FROM question_diagnostic_profile WHERE question_id BETWEEN 9501 AND 9520 ORDER BY question_id;
