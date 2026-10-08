-- =============================================================================
-- V6__rename_completed_concept_count.sql
--
-- user_learning_history_stats.completed_concept_count 를 passed_concept_count 로 바꾼다(#229).
--
-- 이 컬럼은 처음부터 "PASS(전 문제 정답)로 통과한 개념 수"를 쌓아 왔다. 이름만 completed 라
-- "끝까지 푼 개념"으로 읽혀, 엔티티·API 필드(passedConceptCount)와 함께 이름을 맞춘다.
-- 값은 그대로 옮겨지고(RENAME 은 메타데이터만 바뀐다) 다시 계산하지 않는다.
--
-- README 규칙(이름 변경은 두 번에 나눈다)의 예외다. 지금 트래픽은 팀원뿐이라 한 번에 바꾸기로
-- 했다. 대가: 롤링 배포 1~2분 동안 옛 태스크로 간 풀이 완료(모든 유형 — 완료 처리 안에서 이
-- 테이블을 읽는다)와 친구 Learning History 조회가 컬럼을 못 찾아 실패한다. 완료 처리는 한
-- 트랜잭션이라 통째로 롤백되므로 누적값이 어긋나지는 않고, 다시 제출하면 된다.
-- 배포가 실패해 앱만 이전 버전으로 돌아가면 같은 오류가 계속되므로, 그때는 이름을 되돌리는
-- 새 V 파일로 맞춘다.
-- =============================================================================

ALTER TABLE user_learning_history_stats
    RENAME COLUMN completed_concept_count TO passed_concept_count;
