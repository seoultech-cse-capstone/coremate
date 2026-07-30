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

    boolean existsByUserIdAndConcept_ConceptIdAndStatus(Long userId, Long conceptId, SolveSessionStatus status);

    /**
     * 특정 유저의, 주어진 개념들에 대한 특정 유형(개념 문제집 등) 세션을 일괄 조회한다.
     * 개념 문제집 목록의 풀이 상태(미풀이/진행중/완료, 최고 점수 회차 등)를 집계하는 데 사용한다.
     */
    List<SolveSession> findByUserIdAndSessionTypeAndConcept_ConceptIdIn(
            Long userId, SolveSessionType sessionType, List<Long> conceptIds);

    /**
     * 사용자의 완료된 회차 목록을 필터별로 조회한다.
     * 각 필터(subject/categoryLv1/categoryLv2)는 null 이면 조건에서 제외된다.
     * 완료 시간 내림차순으로 정렬한다.
     *
     * 개념 문제집(CONCEPT_PRACTICE) 세션은 subject/categoryLv1/categoryLv2 가 null 이므로
     * 경로 표현식이 유도하는 INNER JOIN 을 피하기 위해 LEFT JOIN 으로 명시적으로 조인한다.
     */
    @Query("""
            SELECT s FROM SolveSession s
            LEFT JOIN s.subject subj
            LEFT JOIN s.categoryLv1 lv1
            LEFT JOIN s.categoryLv2 lv2
            WHERE s.userId = :userId
              AND s.status = :status
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
}
