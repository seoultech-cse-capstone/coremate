package com.coremate.backend.solve.repository;

import com.coremate.backend.solve.entity.SolveLog;
import com.coremate.backend.solve.entity.SolveSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolveLogRepository extends JpaRepository<SolveLog, Long> {

    List<SolveLog> findAllBySolveSessionOrderByQuestionOrder(SolveSession solveSession);

    boolean existsBySolveSessionAndQuestion_QuestionId(SolveSession solveSession, Long questionId);

    long countBySolveSessionAndCorrectTrue(SolveSession solveSession);
}
