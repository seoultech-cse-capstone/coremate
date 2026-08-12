package com.coremate.backend.progress.service;

import com.coremate.backend.content.repository.ConceptRepository;
import com.coremate.backend.progress.dto.response.ConceptCountSummaryResponse;
import com.coremate.backend.solve.repository.SolveSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final ConceptRepository conceptRepository;
    private final SolveSessionRepository solveSessionRepository;

    /**
     * 복습탭 개념 카운팅(총 개념 / 풀었어요 / 아직 못풀었어요)을 전역(과목 무관) 기준으로 집계한다.
     * - 총 개념: 전체 활성 개념 수
     * - 풀었어요: 사용자가 COMPLETED 상태의 개념 문제집 세션을 가진 서로 다른 개념 수
     * - 아직 못풀었어요: 총 개념 - 풀었어요 (미풀이 + 진행중 포함)
     */
    @Transactional(readOnly = true)
    public ConceptCountSummaryResponse getConceptCountSummary(Long userId) {
        long totalConceptCount = conceptRepository.countByActiveTrue();
        long solvedConceptCount = solveSessionRepository.countDistinctSolvedConcepts(userId);
        long unsolvedConceptCount = totalConceptCount - solvedConceptCount;
        return new ConceptCountSummaryResponse(totalConceptCount, solvedConceptCount, unsolvedConceptCount);
    }
}
