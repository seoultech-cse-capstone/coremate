package com.coremate.backend.weakness.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.reward.service.RewardService;
import com.coremate.backend.solve.entity.SolveSession;
import com.coremate.backend.weakness.client.WeaknessAiClient;
import com.coremate.backend.weakness.config.WeaknessAnalysisProperties;
import com.coremate.backend.weakness.dto.request.CreateWeaknessAnalysisReportRequest;
import com.coremate.backend.weakness.dto.type.AnalysisReportStatus;
import com.coremate.backend.weakness.dto.type.AnalysisScopeType;
import com.coremate.backend.weakness.entity.WeaknessAnalysisReport;
import com.coremate.backend.weakness.repository.WeaknessAnalysisAiCallLogRepository;
import com.coremate.backend.weakness.repository.WeaknessAnalysisConceptRepository;
import com.coremate.backend.weakness.repository.WeaknessAnalysisEvidenceLogRepository;
import com.coremate.backend.weakness.repository.WeaknessAnalysisReportRepository;
import com.coremate.backend.weakness.repository.WeaknessAnalysisReportViewLogRepository;
import com.coremate.backend.weakness.repository.WeaknessAnalysisSessionRepository;
import com.coremate.backend.weakness.service.model.WeaknessScoringResult;
import com.coremate.backend.content.repository.ConceptRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

/**
 * 생성 오케스트레이션의 안전 경로(근거 부족·멱등)를 검증한다. AI 호출/코인 차감이
 * 일어나면 안 되는 경로에서 실제로 호출되지 않는지 확인.
 */
@ExtendWith(MockitoExtension.class)
class WeaknessAnalysisServiceTest {

    @Mock private WeaknessSelectionValidator selectionValidator;
    @Mock private WeaknessScoringService scoringService;
    @Mock private WeaknessAiClient aiClient;
    @Mock private RewardService rewardService;
    @Mock private WeaknessAnalysisProperties props;
    @Mock private ObjectMapper objectMapper;
    @Mock private WeaknessAnalysisReportRepository reportRepository;
    @Mock private WeaknessAnalysisSessionRepository sessionRepository;
    @Mock private WeaknessAnalysisConceptRepository conceptRepository;
    @Mock private WeaknessAnalysisEvidenceLogRepository evidenceLogRepository;
    @Mock private WeaknessAnalysisAiCallLogRepository aiCallLogRepository;
    @Mock private WeaknessAnalysisReportViewLogRepository reportViewLogRepository;
    @Mock private ConceptRepository contentConceptRepository;

    @InjectMocks
    private WeaknessAnalysisService service;

    private CreateWeaknessAnalysisReportRequest request() {
        return new CreateWeaknessAnalysisReportRequest(
                List.of(1L, 2L), AnalysisScopeType.SUBJECT, null, null, null, null, null, true);
    }

    @Test
    void insufficientEvidenceStopsBeforeCoinAndAi() {
        when(selectionValidator.validateAndLoad(anyLong(), any()))
                .thenReturn(List.of(mock(SolveSession.class)));
        when(scoringService.score(any()))
                .thenReturn(new WeaknessScoringResult(List.of(), 3, 1, 0, 0, false));

        CoreMateException ex = assertThrows(CoreMateException.class,
                () -> service.createReport(1L, request()));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WEAKNESS_INSUFFICIENT_EVIDENCE);
        verify(aiClient, never()).analyze(any());
        verify(rewardService, never()).spendCoin(anyLong(), anyInt(), any(), any(), any());
    }

    @Test
    void idempotentReuseSkipsCoinAndAi() {
        when(selectionValidator.validateAndLoad(anyLong(), any()))
                .thenReturn(List.of(mock(SolveSession.class)));
        when(scoringService.score(any()))
                .thenReturn(new WeaknessScoringResult(List.of(), 10, 5, 2, 1, true));

        WeaknessAnalysisReport existing = WeaknessAnalysisReport.builder()
                .userId(1L).subjectId(10L).analysisScopeType(AnalysisScopeType.SUBJECT)
                .title("기존 분석표").reportFormat("MARKDOWN").selectedSessionCount(2)
                .totalQuestionCount(10).totalWrongCount(4).totalLongTimeCount(2).totalHintUsedCount(1)
                .coinCost(100).requestFingerprint("fp").build();
        existing.markCompleted("요약", null, "ai", "v1", "v1", "v1", "none");

        when(reportRepository.findByRequestFingerprintAndStatus(anyString(), eq(AnalysisReportStatus.COMPLETED)))
                .thenReturn(Optional.of(existing));
        when(conceptRepository.findByReport_AnalysisReportIdOrderBySortOrderAsc(any())).thenReturn(List.of());
        when(sessionRepository.findByReport_AnalysisReportIdOrderBySelectedOrderAsc(any())).thenReturn(List.of());

        var detail = service.createReport(1L, request());

        assertThat(detail).isNotNull();
        assertThat(detail.status()).isEqualTo(AnalysisReportStatus.COMPLETED);
        verify(aiClient, never()).analyze(any());
        verify(rewardService, never()).spendCoin(anyLong(), anyInt(), any(), any(), any());
    }
}
