package com.coremate.backend.solve.repository;

import com.coremate.backend.solve.dto.type.SolveSessionStatus;
import com.coremate.backend.solve.dto.type.SolveSessionType;
import com.coremate.backend.solve.entity.SolveSession;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolveSessionRepository extends JpaRepository<SolveSession, Long> {

    Optional<SolveSession> findBySolveSessionIdAndUserId(Long solveSessionId, Long userId);

    Optional<SolveSession> findByUserIdAndConcept_ConceptIdAndStatus(Long userId, Long conceptId, SolveSessionStatus status);

    /**
     * 특정 유저의, 주어진 개념들에 대한 특정 유형(개념 문제집 등) 세션을 일괄 조회한다.
     * 개념 문제집 목록의 풀이 상태(미풀이/진행중/완료, 최고 점수 회차 등)를 집계하는 데 사용한다.
     */
    List<SolveSession> findByUserIdAndSessionTypeAndConcept_ConceptIdIn(
            Long userId, SolveSessionType sessionType, List<Long> conceptIds);

    /**
     * 사용자의 완료된 "문제집(모의고사)" 회차 목록을 필터별로 조회한다.
     * 복습 탭의 "내가 푼 문제집"은 모의고사 회차만 노출하므로 개념 문제집(CONCEPT_PRACTICE)은 제외한다.
     * 각 필터(subject/categoryLv1/categoryLv2)는 null 이면 조건에서 제외된다.
     * 완료 시간 내림차순으로 정렬한다.
     *
     * 과목 모의고사 등은 subject 는 있으나 categoryLv1/categoryLv2 가 null 이므로
     * 경로 표현식이 유도하는 INNER JOIN 을 피하기 위해 LEFT JOIN 으로 명시적으로 조인한다.
     */
    @Query("""
            SELECT s FROM SolveSession s
            LEFT JOIN s.subject subj
            LEFT JOIN s.categoryLv1 lv1
            LEFT JOIN s.categoryLv2 lv2
            WHERE s.userId = :userId
              AND s.status = :status
              AND s.sessionType <> com.coremate.backend.solve.dto.type.SolveSessionType.CONCEPT_PRACTICE
              AND (:subjectId IS NULL OR subj.subjectId = :subjectId)
              AND (:categoryLv1Id IS NULL OR lv1.categoryLv1Id = :categoryLv1Id)
              AND (:categoryLv2Id IS NULL OR lv2.categoryLv2Id = :categoryLv2Id)
            ORDER BY s.completedAt DESC
            """)
    List<SolveSession> findCompletedSessions(
            @Param("userId") Long userId,
            @Param("status") SolveSessionStatus status,
            @Param("subjectId") Long subjectId,
            @Param("categoryLv1Id") Long categoryLv1Id,
            @Param("categoryLv2Id") Long categoryLv2Id
    );

    /**
     * 사용자가 현재 세션 외에 해당 개념을 이미 PASS(전 문제 정답)로 완료한 회차가 있는지 확인한다.
     * Learning History의 "최초 PASS 시에만 완료 개념 +1" 판정에 사용한다.
     */
    @Query("""
            SELECT (COUNT(s) > 0) FROM SolveSession s
            WHERE s.userId = :userId
              AND s.concept.conceptId = :conceptId
              AND s.status = com.coremate.backend.solve.dto.type.SolveSessionStatus.COMPLETED
              AND s.solveSessionId <> :excludeSessionId
              AND s.totalQuestionCount > 0
              AND s.correctCount = s.totalQuestionCount
            """)
    boolean existsPassedConceptSession(
            @Param("userId") Long userId,
            @Param("conceptId") Long conceptId,
            @Param("excludeSessionId") Long excludeSessionId);

    // 약점분석 대상 후보: 사용자의 완료된 모의고사 회차(개념 문제집·오늘의 퀴즈 제외)
    List<SolveSession> findByUserIdAndStatusAndSessionTypeInOrderByCompletedAtDesc(
            Long userId, SolveSessionStatus status, Collection<SolveSessionType> sessionTypes);

    // 선택 검증: 사용자 소유의 특정 회차들 조회
    List<SolveSession> findBySolveSessionIdInAndUserId(Collection<Long> solveSessionIds, Long userId);

    // 진행 중 회차 조회. 새 모의고사 시작 시 기존 모의고사 회차를 중단 처리하는 데 사용한다.
    List<SolveSession> findByUserIdAndStatusAndSessionTypeIn(
            Long userId, SolveSessionStatus status, Collection<SolveSessionType> sessionTypes);

    /**
     * 복습탭 개념 카운팅의 "풀었어요": 사용자가 합격(PASS)한 개념 문제집 세션을 가진 서로 다른 개념 수.
     * SCR-LOG-01 이 요구하는 기준은 완료(COMPLETED)가 아니라 합격이다. 끝까지 풀기만 하고 틀린 개념은
     * "아직 못풀었어요"에 남는다. 문제가 0개인 회차가 합격으로 오판되지 않도록 총 문제 수 > 0 을 함께 건다.
     *
     * <p>비활성 개념은 제외한다. "총 개념"(countByActiveTrue)이 활성만 세므로, 합격한 뒤 비활성으로
     * 바뀐 개념을 여기서 세면 풀었어요가 총 개념을 넘어 "아직 못풀었어요"가 음수가 될 수 있다.
     */
    @Query("""
            SELECT COUNT(DISTINCT s.concept.conceptId) FROM SolveSession s
            WHERE s.userId = :userId
              AND s.sessionType = com.coremate.backend.solve.dto.type.SolveSessionType.CONCEPT_PRACTICE
              AND s.status = com.coremate.backend.solve.dto.type.SolveSessionStatus.COMPLETED
              AND s.totalQuestionCount > 0
              AND s.correctCount = s.totalQuestionCount
              AND s.concept.active = true
            """)
    long countDistinctSolvedConcepts(@Param("userId") Long userId);

    /**
     * 과목별 학습 달성률의 "합격한 개념": 해당 과목 하위에서 합격(PASS)한 개념 문제집 세션을 가진
     * 서로 다른 개념 수. FR-STAT-02 가 정한 기준이며, 복습탭 카운팅(countDistinctSolvedConcepts)과 같다.
     * 분모(countByCategoryLv2_..._AndActiveTrue)가 활성 개념만 세므로 여기서도 비활성 개념을 뺀다.
     */
    @Query("""
            SELECT COUNT(DISTINCT s.concept.conceptId) FROM SolveSession s
            WHERE s.userId = :userId
              AND s.sessionType = com.coremate.backend.solve.dto.type.SolveSessionType.CONCEPT_PRACTICE
              AND s.status = com.coremate.backend.solve.dto.type.SolveSessionStatus.COMPLETED
              AND s.totalQuestionCount > 0
              AND s.correctCount = s.totalQuestionCount
              AND s.concept.active = true
              AND s.concept.categoryLv2.categoryLv1.subject.subjectId = :subjectId
            """)
    long countDistinctSolvedConceptsBySubject(@Param("userId") Long userId, @Param("subjectId") Long subjectId);

    /**
     * 문제집 완료 상태(모의고사)용: 지정 유형·부모 필터에 맞는 COMPLETED 모의고사 회차.
     * 대상 레벨 id(subject/lv1/lv2)는 유형에 따라 세션에 채워진 값을 서비스에서 읽어 targetId로 쓴다.
     * 개념 문제집(CONCEPT_PRACTICE)은 subject/lv1/lv2 가 null 이라 이 쿼리 대상이 아니다.
     */
    @Query("""
            SELECT s FROM SolveSession s
            LEFT JOIN s.subject subj
            LEFT JOIN s.categoryLv1 lv1
            LEFT JOIN s.categoryLv2 lv2
            WHERE s.userId = :userId
              AND s.status = com.coremate.backend.solve.dto.type.SolveSessionStatus.COMPLETED
              AND s.sessionType = :sessionType
              AND (:subjectId IS NULL OR subj.subjectId = :subjectId)
              AND (:categoryLv1Id IS NULL OR lv1.categoryLv1Id = :categoryLv1Id)
              AND (:categoryLv2Id IS NULL OR lv2.categoryLv2Id = :categoryLv2Id)
            ORDER BY s.completedAt DESC
            """)
    List<SolveSession> findCompletedMockSessions(
            @Param("userId") Long userId,
            @Param("sessionType") SolveSessionType sessionType,
            @Param("subjectId") Long subjectId,
            @Param("categoryLv1Id") Long categoryLv1Id,
            @Param("categoryLv2Id") Long categoryLv2Id);

    /**
     * 문제집 완료 상태(개념 문제집)용: 지정 중분류 하위 개념들의 COMPLETED 개념 문제집 회차.
     * 개념 문제집 세션은 concept 만 채워지므로 중분류 필터는 concept.categoryLv2 경로로 건다.
     */
    @Query("""
            SELECT s FROM SolveSession s
            WHERE s.userId = :userId
              AND s.status = com.coremate.backend.solve.dto.type.SolveSessionStatus.COMPLETED
              AND s.sessionType = com.coremate.backend.solve.dto.type.SolveSessionType.CONCEPT_PRACTICE
              AND (:categoryLv2Id IS NULL OR s.concept.categoryLv2.categoryLv2Id = :categoryLv2Id)
            ORDER BY s.completedAt DESC
            """)
    List<SolveSession> findCompletedConceptPracticeSessions(
            @Param("userId") Long userId,
            @Param("categoryLv2Id") Long categoryLv2Id);
}
