package com.coremate.backend.solve.repository;

import com.coremate.backend.solve.dto.type.SolveSessionStatus;
import com.coremate.backend.solve.entity.SolveSession;
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
     * 사용자의 완료된 회차 목록을 필터별로 조회한다.
     * 각 필터(subject/categoryLv1/categoryLv2)는 null 이면 조건에서 제외된다.
     * 완료 시간 내림차순으로 정렬한다.
     */
    @Query("""
            SELECT s FROM SolveSession s
            WHERE s.userId = :userId
              AND s.status = :status
              AND (:subjectId IS NULL OR s.subject.subjectId = :subjectId)
              AND (:categoryLv1Id IS NULL OR s.categoryLv1.categoryLv1Id = :categoryLv1Id)
              AND (:categoryLv2Id IS NULL OR s.categoryLv2.categoryLv2Id = :categoryLv2Id)
            ORDER BY s.completedAt DESC
            """)
    List<SolveSession> findCompletedSessions(
            @Param("userId") Long userId,
            @Param("status") SolveSessionStatus status,
            @Param("subjectId") Long subjectId,
            @Param("categoryLv1Id") Long categoryLv1Id,
            @Param("categoryLv2Id") Long categoryLv2Id
    );
}
