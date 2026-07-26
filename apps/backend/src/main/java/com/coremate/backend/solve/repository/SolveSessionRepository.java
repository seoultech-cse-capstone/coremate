package com.coremate.backend.solve.repository;

import com.coremate.backend.solve.dto.type.SolveSessionStatus;
import com.coremate.backend.solve.dto.type.SolveSessionType;
import com.coremate.backend.solve.entity.SolveSession;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolveSessionRepository extends JpaRepository<SolveSession, Long> {

    Optional<SolveSession> findBySolveSessionIdAndUserId(Long solveSessionId, Long userId);

    Optional<SolveSession> findByUserIdAndConcept_ConceptIdAndStatus(Long userId, Long conceptId, SolveSessionStatus status);

    boolean existsByUserIdAndConcept_ConceptIdAndStatus(Long userId, Long conceptId, SolveSessionStatus status);

    // 약점분석 대상 후보: 사용자의 완료된 모의고사 회차(개념 문제집·오늘의 퀴즈 제외)
    List<SolveSession> findByUserIdAndStatusAndSessionTypeInOrderByCompletedAtDesc(
            Long userId, SolveSessionStatus status, Collection<SolveSessionType> sessionTypes);

    // 선택 검증: 사용자 소유의 특정 회차들 조회
    List<SolveSession> findBySolveSessionIdInAndUserId(Collection<Long> solveSessionIds, Long userId);
}
