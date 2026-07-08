package com.coremate.backend.content.repository;

import com.coremate.backend.content.entity.Question;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findAllByConcept_ConceptIdAndActiveTrueAndConceptPracticeEligibleTrueOrderByQuestionIdAsc(
            Long conceptId);

    Optional<Question> findByQuestionIdAndConcept_ConceptIdAndActiveTrueAndConceptPracticeEligibleTrue(
            Long questionId, Long conceptId);

    @Query(value = """
            SELECT * FROM question
            WHERE concept_id = :conceptId
              AND is_active = true
              AND is_concept_practice_eligible = true
            ORDER BY RAND()
            LIMIT :limit
            """, nativeQuery = true)
    List<Question> findRandomByConceptId(@Param("conceptId") Long conceptId, @Param("limit") int limit);

    @Query(value = """
            SELECT q.* FROM question q
            LEFT JOIN solve_logs sl
              ON q.question_id = sl.question_id AND sl.user_id = :userId
            WHERE q.concept_id = :conceptId
              AND q.is_active = true
              AND q.is_concept_practice_eligible = true
              AND sl.solve_log_id IS NULL
            ORDER BY RAND()
            LIMIT :limit
            """, nativeQuery = true)
    List<Question> findUnsolvedByConceptId(
            @Param("conceptId") Long conceptId,
            @Param("userId") Long userId,
            @Param("limit") int limit
    );

    @Query(value = """
            SELECT * FROM question
            WHERE concept_id IN (:conceptIds)
              AND is_active = true
              AND is_mock_eligible = true
            ORDER BY RAND()
            LIMIT :limit
            """, nativeQuery = true)
    List<Question> findRandomMockQuestionsByConceptIds(
            @Param("conceptIds") List<Long> conceptIds,
            @Param("limit") int limit
    );

    @Query(value = """
            SELECT * FROM question
            WHERE concept_id IN (:conceptIds)
              AND is_active = true
              AND is_mock_eligible = true
              AND question_id NOT IN (:excludeQuestionIds)
            ORDER BY RAND()
            LIMIT :limit
            """, nativeQuery = true)
    List<Question> findRandomMockQuestionsByConceptIdsExcluding(
            @Param("conceptIds") List<Long> conceptIds,
            @Param("excludeQuestionIds") List<Long> excludeQuestionIds,
            @Param("limit") int limit
    );
}
