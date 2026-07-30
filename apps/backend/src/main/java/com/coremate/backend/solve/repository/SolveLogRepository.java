package com.coremate.backend.solve.repository;

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

    // 약점분석: 선택된 여러 회차의 문항별 로그를 한 번에 조회
    List<SolveLog> findAllBySolveSession_SolveSessionIdInOrderByQuestionOrder(Collection<Long> solveSessionIds);
}
