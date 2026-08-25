package com.coremate.backend.weakness.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.assertj.core.api.Assertions.tuple;
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
import com.coremate.backend.paid.service.FeatureAccessPricingService;
import com.coremate.backend.reward.service.RewardService;
import com.coremate.backend.solve.entity.SolveSession;
import com.coremate.backend.solve.repository.SolveLogRepository;
import com.coremate.backend.solve.repository.SolveSessionRepository;
import com.coremate.backend.weakness.client.WeaknessAiClient;
import com.coremate.backend.weakness.config.WeaknessAnalysisProperties;
import com.coremate.backend.weakness.dto.ai.AiWeaknessAnalysisRequest;
import com.coremate.backend.weakness.dto.ai.AiWeaknessAnalysisResponse;
import com.coremate.backend.weakness.dto.type.AiAnalysisStatus;
import com.coremate.backend.weakness.entity.WeaknessAnalysisEvidenceLog;
import com.coremate.backend.weakness.dto.request.CreateWeaknessAnalysisReportRequest;
import com.coremate.backend.weakness.dto.response.WeaknessEvidenceLogResponse;
import com.coremate.backend.weakness.dto.type.WeaknessType;
import com.coremate.backend.weakness.service.model.ConceptCandidate;
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
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    @Mock private DiagnosticProfileService diagnosticProfileService;
    @Mock private WeaknessSelectionValidator selectionValidator;
    @Mock private WeaknessScoringService scoringService;
    @Mock private WeaknessAiClient aiClient;
    @Mock private RewardService rewardService;
    @Mock private WeaknessAnalysisProperties props;
    @Mock private FeatureAccessPricingService featureAccessPricingService;
    @Mock private ObjectMapper objectMapper;
    @Mock private WeaknessAnalysisReportRepository reportRepository;
    @Mock private WeaknessAnalysisSessionRepository sessionRepository;
    @Mock private WeaknessAnalysisConceptRepository conceptRepository;
    @Mock private WeaknessAnalysisEvidenceLogRepository evidenceLogRepository;
    @Mock private WeaknessAnalysisAiCallLogRepository aiCallLogRepository;
    @Mock private WeaknessAnalysisReportViewLogRepository reportViewLogRepository;
    @Mock private ConceptRepository contentConceptRepository;
    @Mock private SolveLogRepository solveLogRepository;
    @Mock private SolveSessionRepository weaknessSolveSessionRepository;

    @InjectMocks
    private WeaknessAnalysisService service;

    @Test
    void 생성비용_사전조회는_실제_차감과_같은_정책조회를_쓴다() {
        when(props.getCoinCost()).thenReturn(100);
        when(featureAccessPricingService.priceCoinOf(
                FeatureAccessPricingService.WEAKNESS_ANALYSIS_CREATE, false, 100)).thenReturn(80);

        assertThat(service.getCreateCoinCost()).isEqualTo(80);
    }

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

    /**
     * 진단 프로필이 AI 입력에 실제로 실리는지 검증한다(C1 배선 회귀 방지).
     * 같은 오개념 코드가 2회 나오면 repeated_misconceptions 로 집계돼야 한다.
     */
    @Test
    void aiRequestCarriesDiagnosticProfileSignals() {
        SolveSession session = mock(SolveSession.class);
        when(selectionValidator.validateAndLoad(anyLong(), any())).thenReturn(List.of(session));

        WeaknessEvidenceLogResponse wrongA = evidence(9501L, "WRONG", "B");
        WeaknessEvidenceLogResponse wrongB = evidence(9502L, "WRONG", "C");
        WeaknessEvidenceLogResponse slow = evidence(9503L, "LONG_TIME", "A");
        ConceptCandidate candidate = new ConceptCandidate(9301L, "완전 이진 트리", "한 줄 정의",
                5, 2, 1, 0, 40, new BigDecimal("0.800"), new BigDecimal("0.200"),
                WeaknessType.CONCEPT_CONFUSION, List.of(wrongA, wrongB, slow));
        when(scoringService.score(any()))
                .thenReturn(new WeaknessScoringResult(List.of(candidate), 10, 2, 1, 0, true));
        when(reportRepository.findByRequestFingerprintAndStatus(anyString(), any()))
                .thenReturn(Optional.empty());
        when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(conceptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 두 오답이 같은 오개념 코드를 가리키고, LONG_TIME 문항은 코드가 없다.
        when(diagnosticProfileService.loadQuestionProfiles(any())).thenReturn(Map.of(
                9501L, profileView("RECALL", Map.of("B", "CPT_M01")),
                9502L, profileView("APPLICATION", Map.of("C", "CPT_M01")),
                9503L, profileView("ANALYSIS", Map.of())));
        when(diagnosticProfileService.loadConceptProfile(9301L)).thenReturn(Optional.of(
                new DiagnosticProfileService.ConceptProfileView("프로필 핵심", "프로필 설명",
                        List.of(), List.of(), List.of(), null, null, List.of())));
        when(aiClient.analyze(any())).thenReturn(new AiWeaknessAnalysisResponse("요약", List.of()));

        service.createReport(1L, request());

        ArgumentCaptor<AiWeaknessAnalysisRequest> captor =
                ArgumentCaptor.forClass(AiWeaknessAnalysisRequest.class);
        verify(aiClient).analyze(captor.capture());
        AiWeaknessAnalysisRequest.Candidate sent = captor.getValue().weaknessCandidates().get(0);

        assertThat(sent.conceptCore()).isEqualTo("프로필 핵심");
        assertThat(sent.evidence()).extracting(AiWeaknessAnalysisRequest.Evidence::skillType)
                .containsExactly("RECALL", "APPLICATION", "ANALYSIS");
        assertThat(sent.evidence()).extracting(AiWeaknessAnalysisRequest.Evidence::misconceptionCode)
                .containsExactly("CPT_M01", "CPT_M01", null);
        assertThat(sent.repeatedMisconceptions()).hasSize(1);
        assertThat(sent.repeatedMisconceptions().get(0).code()).isEqualTo("CPT_M01");
        assertThat(sent.repeatedMisconceptions().get(0).count()).isEqualTo(2);
    }

    /**
     * AI 응답의 evidence_refs 가 실제 근거 로그에 되돌려 붙는지 검증한다.
     *
     * <p>ref 는 요청을 만들 때 근거 순서대로 매긴 값이라, 짝이 어긋나면 엉뚱한 문제가
     * "AI가 지목한 근거"로 표시된다. 화면에 그대로 노출되는 값이라 회귀를 막아야 한다.
     */
    @Test
    void aiEvidenceRefsAreMappedBackToEvidenceLogs() {
        when(selectionValidator.validateAndLoad(anyLong(), any())).thenReturn(List.of(mock(SolveSession.class)));

        WeaknessEvidenceLogResponse first = evidence(9601L, "WRONG", "B");
        WeaknessEvidenceLogResponse second = evidence(9602L, "WRONG", "C");
        WeaknessEvidenceLogResponse third = evidence(9603L, "LONG_TIME", "A");
        ConceptCandidate candidate = new ConceptCandidate(9401L, "빅오 표기법", "한 줄 정의",
                5, 2, 1, 0, 60, new BigDecimal("0.800"), new BigDecimal("0.200"),
                WeaknessType.CONCEPT_CONFUSION, List.of(first, second, third));
        when(scoringService.score(any()))
                .thenReturn(new WeaknessScoringResult(List.of(candidate), 10, 2, 1, 0, true));
        when(reportRepository.findByRequestFingerprintAndStatus(anyString(), any())).thenReturn(Optional.empty());
        when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(conceptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // AI 는 1번·3번 근거만 지목했다(e1, e3).
        when(aiClient.analyze(any())).thenReturn(new AiWeaknessAnalysisResponse("요약", List.of(
                new AiWeaknessAnalysisResponse.ConceptAnalysis(9401L, AiAnalysisStatus.SUPPORTED,
                        WeaknessType.CONCEPT_CONFUSION, List.of(), null, null, "진단", null, null,
                        List.of("e1", "e3")))));

        var detail = service.createReport(1L, request());

        assertThat(detail.weaknessConcepts().get(0).evidenceLogs())
                .extracting(WeaknessEvidenceLogResponse::questionId, WeaknessEvidenceLogResponse::citedByAi)
                .containsExactly(tuple(9601L, true), tuple(9602L, false), tuple(9603L, true));

        ArgumentCaptor<WeaknessAnalysisEvidenceLog> saved =
                ArgumentCaptor.forClass(WeaknessAnalysisEvidenceLog.class);
        verify(evidenceLogRepository, times(3)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(WeaknessAnalysisEvidenceLog::isCitedByAi)
                .containsExactly(true, false, true);
    }

    private WeaknessEvidenceLogResponse evidence(Long questionId, String type, String selectedAnswer) {
        return new WeaknessEvidenceLogResponse(null, questionId, questionId, "문제",
                type, 60, 60, 0, selectedAnswer, "A", !"WRONG".equals(type), false);
    }

    private DiagnosticProfileService.QuestionProfileView profileView(String skillType,
            Map<String, String> misconceptionByOption) {
        return new DiagnosticProfileService.QuestionProfileView(skillType, misconceptionByOption);
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
