package com.coremate.backend.solve.repository;

import com.coremate.backend.solve.dto.response.SubjectQuestionCount;
import com.coremate.backend.solve.entity.SolveLog;
import com.coremate.backend.solve.entity.SolveSession;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolveLogRepository extends JpaRepository<SolveLog, Long> {

    List<SolveLog> findAllBySolveSessionOrderByQuestionOrder(SolveSession solveSession);

    /** 약점 분석표 재열람에서 근거 로그의 문제 본문·답안을 함께 채우기 위한 조회. */
    @Query("SELECT l FROM SolveLog l JOIN FETCH l.question WHERE l.solveLogId IN :solveLogIds")
    List<SolveLog> findAllWithQuestionBySolveLogIdIn(@Param("solveLogIds") Collection<Long> solveLogIds);

    boolean existsBySolveSessionAndQuestion_QuestionId(SolveSession solveSession, Long questionId);

    long countBySolveSessionAndCorrectTrue(SolveSession solveSession);

    long countBySolveSession(SolveSession solveSession);

    /**
     * 사용자가 현재 세션 밖에서 이미 정답 처리한 questionId 집합을 반환한다.
     * Learning History의 "새로 맞힌 distinct 문제 수" 계산에 사용한다.
     */
    @Query("""
            SELECT DISTINCT l.question.questionId FROM SolveLog l
            WHERE l.userId = :userId
              AND l.correct = true
              AND l.solveSession.solveSessionId <> :excludeSessionId
              AND l.question.questionId IN :questionIds
            """)
    Set<Long> findCorrectlyAnsweredQuestionIds(
            @Param("userId") Long userId,
            @Param("questionIds") Collection<Long> questionIds,
            @Param("excludeSessionId") Long excludeSessionId);

    /**
     * 과목별 "푼 문제 수 / 맞힌 문제 수"를 한 번의 GROUP BY 로 집계한다(FR-STAT-01 과목별 스탯).
     *
     * <p>과목은 세션이 아니라 <b>문제의 개념 계층</b>(question -> concept -> categoryLv2 ->
     * categoryLv1 -> subject)으로 판정한다. {@code solve_sessions.subject} 는 과목 모의고사에만
     * 채워지고 개념 문제집은 null 이라 그것으로 묶으면 개념 문제집이 통째로 빠진다.
     *
     * <p>COMPLETED 회차의 로그만 센다. 이탈(ABANDONED) 회차는 풀이 로그를 보존하되 결과 산출
     * 대상이 아니고(EX-LEARN-005), Learning History 누적도 COMPLETED 전환 시점을 기준으로 한다.
     */
    @Query("""
            SELECT new com.coremate.backend.solve.dto.response.SubjectQuestionCount(
                       l.question.concept.categoryLv2.categoryLv1.subject.subjectId,
                       COUNT(l),
                       SUM(CASE WHEN l.correct = true THEN 1L ELSE 0L END))
            FROM SolveLog l
            WHERE l.userId = :userId
              AND l.solveSession.status = com.coremate.backend.solve.dto.type.SolveSessionStatus.COMPLETED
            GROUP BY l.question.concept.categoryLv2.categoryLv1.subject.subjectId
            """)
    List<SubjectQuestionCount> aggregateQuestionCountsBySubject(@Param("userId") Long userId);

    // 약점분석: 선택된 여러 회차의 문항별 로그를 한 번에 조회
    List<SolveLog> findAllBySolveSession_SolveSessionIdInOrderByQuestionOrder(Collection<Long> solveSessionIds);
}
