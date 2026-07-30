package com.coremate.backend.solve.repository;

import com.coremate.backend.solve.entity.SolveLog;
import com.coremate.backend.solve.entity.SolveSession;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolveLogRepository extends JpaRepository<SolveLog, Long> {

    List<SolveLog> findAllBySolveSessionOrderByQuestionOrder(SolveSession solveSession);

    boolean existsBySolveSessionAndQuestion_QuestionId(SolveSession solveSession, Long questionId);

    long countBySolveSessionAndCorrectTrue(SolveSession solveSession);

    long countBySolveSession(SolveSession solveSession);

    // 약점분석: 선택된 여러 회차의 문항별 로그를 한 번에 조회
    List<SolveLog> findAllBySolveSession_SolveSessionIdInOrderByQuestionOrder(Collection<Long> solveSessionIds);
}
