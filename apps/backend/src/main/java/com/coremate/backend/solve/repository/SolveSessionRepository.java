package com.coremate.backend.solve.repository;

import com.coremate.backend.solve.dto.type.SolveSessionStatus;
import com.coremate.backend.solve.entity.SolveSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolveSessionRepository extends JpaRepository<SolveSession, Long> {

    Optional<SolveSession> findBySolveSessionIdAndUserId(Long solveSessionId, Long userId);

    Optional<SolveSession> findByUserIdAndConcept_ConceptIdAndStatus(Long userId, Long conceptId, SolveSessionStatus status);

    boolean existsByUserIdAndConcept_ConceptIdAndStatus(Long userId, Long conceptId, SolveSessionStatus status);
}
