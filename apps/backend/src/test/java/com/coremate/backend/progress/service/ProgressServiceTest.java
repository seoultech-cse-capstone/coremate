package com.coremate.backend.progress.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.coremate.backend.content.repository.ConceptRepository;
import com.coremate.backend.progress.dto.response.ConceptCountSummaryResponse;
import com.coremate.backend.solve.repository.SolveSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock
    private ConceptRepository conceptRepository;

    @Mock
    private SolveSessionRepository solveSessionRepository;

    @InjectMocks
    private ProgressService progressService;

    @Test
    void 개념_카운팅은_전체에서_풀었어요를_뺀_값을_아직_못풀었어요로_계산한다() {
        long userId = 1L;
        when(conceptRepository.countByActiveTrue()).thenReturn(500L);
        when(solveSessionRepository.countDistinctSolvedConcepts(userId)).thenReturn(100L);

        ConceptCountSummaryResponse result = progressService.getConceptCountSummary(userId);

        assertThat(result.totalConceptCount()).isEqualTo(500L);
        assertThat(result.solvedConceptCount()).isEqualTo(100L);
        assertThat(result.unsolvedConceptCount()).isEqualTo(400L);
    }

    @Test
    void 풀이한_개념이_없으면_전체가_아직_못풀었어요가_된다() {
        long userId = 2L;
        when(conceptRepository.countByActiveTrue()).thenReturn(42L);
        when(solveSessionRepository.countDistinctSolvedConcepts(userId)).thenReturn(0L);

        ConceptCountSummaryResponse result = progressService.getConceptCountSummary(userId);

        assertThat(result.totalConceptCount()).isEqualTo(42L);
        assertThat(result.solvedConceptCount()).isEqualTo(0L);
        assertThat(result.unsolvedConceptCount()).isEqualTo(42L);
    }
}
