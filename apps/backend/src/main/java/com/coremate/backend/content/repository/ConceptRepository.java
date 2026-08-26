package com.coremate.backend.content.repository;

import com.coremate.backend.content.entity.Concept;
import com.coremate.backend.progress.dto.response.ConceptPassCount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConceptRepository extends JpaRepository<Concept, Long> {

    List<Concept> findAllByCategoryLv2_CategoryLv2IdAndActiveTrueOrderBySortOrderAsc(Long categoryLv2Id);

    Optional<Concept> findByConceptIdAndActiveTrue(Long conceptId);

    List<Concept> findAllByCategoryLv2_CategoryLv1_CategoryLv1IdAndActiveTrue(Long categoryLv1Id);

    List<Concept> findAllByCategoryLv2_CategoryLv1_Subject_SubjectIdAndActiveTrue(Long subjectId);

    // 복습탭 개념 카운팅의 "총 개념": 전체 활성 개념 수
    long countByActiveTrue();

    // 과목별 진행률의 "전체 개념 수": 해당 과목 하위의 전체 활성 개념 수
    long countByCategoryLv2_CategoryLv1_Subject_SubjectIdAndActiveTrue(Long subjectId);

    /**
     * 과목별 개념 합격 집계. 활성 과목 전체를 한 번에 센다.
     *
     * <p>활성 개념을 범위로 묶어 세면서, 그 개념을 사용자가 합격(PASS)한 회차가 있으면 분자로도 센다.
     * LEFT JOIN 이라 아직 아무것도 안 푼 범위도 {@code passedConceptCount = 0} 인 행으로 나온다 —
     * 모의고사 응시 이력과 무관하게 진행률이 계산되어야 하므로 이 점이 중요하다.
     *
     * <p>비활성 범위는 아예 내려주지 않는다. 화면이 활성 과목/대분류/중분류만 나열하므로,
     * 사용자가 들어갈 수 없는 범위의 진행률은 쓸 데가 없다.
     */
    @Query("""
            SELECT new com.coremate.backend.progress.dto.response.ConceptPassCount(
                       c.categoryLv2.categoryLv1.subject.subjectId,
                       COUNT(DISTINCT c.conceptId),
                       COUNT(DISTINCT s.concept.conceptId))
            FROM Concept c
            LEFT JOIN SolveSession s
                   ON s.concept.conceptId = c.conceptId
                  AND s.userId = :userId
                  AND s.sessionType = com.coremate.backend.solve.dto.type.SolveSessionType.CONCEPT_PRACTICE
                  AND s.status = com.coremate.backend.solve.dto.type.SolveSessionStatus.COMPLETED
                  AND s.totalQuestionCount > 0
                  AND s.correctCount = s.totalQuestionCount
            WHERE c.active = true
              AND c.categoryLv2.categoryLv1.subject.active = true
            GROUP BY c.categoryLv2.categoryLv1.subject.subjectId
            """)
    List<ConceptPassCount> aggregatePassCountBySubject(@Param("userId") Long userId);

    /** 대분류별 개념 합격 집계. 지정 과목 하위만 센다. */
    @Query("""
            SELECT new com.coremate.backend.progress.dto.response.ConceptPassCount(
                       c.categoryLv2.categoryLv1.categoryLv1Id,
                       COUNT(DISTINCT c.conceptId),
                       COUNT(DISTINCT s.concept.conceptId))
            FROM Concept c
            LEFT JOIN SolveSession s
                   ON s.concept.conceptId = c.conceptId
                  AND s.userId = :userId
                  AND s.sessionType = com.coremate.backend.solve.dto.type.SolveSessionType.CONCEPT_PRACTICE
                  AND s.status = com.coremate.backend.solve.dto.type.SolveSessionStatus.COMPLETED
                  AND s.totalQuestionCount > 0
                  AND s.correctCount = s.totalQuestionCount
            WHERE c.active = true
              AND c.categoryLv2.categoryLv1.active = true
              AND c.categoryLv2.categoryLv1.subject.subjectId = :subjectId
            GROUP BY c.categoryLv2.categoryLv1.categoryLv1Id
            """)
    List<ConceptPassCount> aggregatePassCountByCategoryLv1(
            @Param("userId") Long userId, @Param("subjectId") Long subjectId);

    /** 중분류별 개념 합격 집계. 지정 대분류 하위만 센다. */
    @Query("""
            SELECT new com.coremate.backend.progress.dto.response.ConceptPassCount(
                       c.categoryLv2.categoryLv2Id,
                       COUNT(DISTINCT c.conceptId),
                       COUNT(DISTINCT s.concept.conceptId))
            FROM Concept c
            LEFT JOIN SolveSession s
                   ON s.concept.conceptId = c.conceptId
                  AND s.userId = :userId
                  AND s.sessionType = com.coremate.backend.solve.dto.type.SolveSessionType.CONCEPT_PRACTICE
                  AND s.status = com.coremate.backend.solve.dto.type.SolveSessionStatus.COMPLETED
                  AND s.totalQuestionCount > 0
                  AND s.correctCount = s.totalQuestionCount
            WHERE c.active = true
              AND c.categoryLv2.active = true
              AND c.categoryLv2.categoryLv1.categoryLv1Id = :categoryLv1Id
            GROUP BY c.categoryLv2.categoryLv2Id
            """)
    List<ConceptPassCount> aggregatePassCountByCategoryLv2(
            @Param("userId") Long userId, @Param("categoryLv1Id") Long categoryLv1Id);
}
