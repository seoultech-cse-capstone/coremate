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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.global.idempotency.IdempotencyGuard;
import com.coremate.backend.paid.service.FeatureAccessPricingService;
import com.coremate.backend.reward.entity.type.CurrencyReason;
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
import com.coremate.backend.weakness.dto.response.WeaknessAnalysisReportDetailResponse;
import com.coremate.backend.weakness.dto.response.RelatedConceptResponse;
import com.coremate.backend.weakness.dto.response.WeaknessConceptResponse;
import com.coremate.backend.weakness.dto.type.CauseCode;
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
import com.coremate.backend.content.entity.CommonMistake;
import com.coremate.backend.content.entity.Concept;
import com.coremate.backend.content.entity.ConceptRelation;
import com.coremate.backend.content.repository.CommonMistakeRepository;
import com.coremate.backend.content.repository.ConceptRelationRepository;
import com.coremate.backend.content.repository.ConceptRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
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
    @Mock private CommonMistakeRepository commonMistakeRepository;
    @Mock private ConceptRelationRepository conceptRelationRepository;
    @Mock private SolveLogRepository solveLogRepository;
    @Mock private SolveSessionRepository weaknessSolveSessionRepository;
    @Mock private IdempotencyGuard idempotencyGuard;
    @Mock private TransactionTemplate transactionTemplate;

    @InjectMocks
    private WeaknessAnalysisService service;

    // 트랜잭션 분할(#320) 이후 생성 로직은 TransactionTemplate 으로 짧은 트랜잭션을 연다.
    // 단위 테스트에선 실제 트랜잭션이 없으므로, 콜백을 그 자리에서 실행하도록 스텁한다.
    @BeforeEach
    void stubTransactionTemplate() {
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        lenient().doAnswer(invocation -> {
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // 트랜잭션 분할 후 저장 tx 는 리포트를 findById 로 재로드한다. 준비 tx 가 저장한 엔티티를
        // 기억해 findById 로도 돌려줘, 단위 테스트에서 재로드가 같은 인스턴스를 가리키게 한다.
        WeaknessAnalysisReport[] savedReport = new WeaknessAnalysisReport[1];
        lenient().when(reportRepository.save(any(WeaknessAnalysisReport.class))).thenAnswer(invocation -> {
            savedReport[0] = invocation.getArgument(0);
            return savedReport[0];
        });
        lenient().when(reportRepository.findById(any()))
                .thenAnswer(invocation -> Optional.ofNullable(savedReport[0]));
    }

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
     * 트랜잭션 분할(#320) 이후, AI 호출이 트랜잭션 밖에서 실패하면 자동 롤백이 없으므로
     * 보상 단계가 코인을 환불하고 리포트를 FAILED 로 바꾸며 멱등키를 해제해야 한다.
     */
    @Test
    void aiFailureRefundsCoinUnregistersKeyAndMarksFailed() {
        when(props.getCoinCost()).thenReturn(100);
        when(featureAccessPricingService.priceCoinOf(
                FeatureAccessPricingService.WEAKNESS_ANALYSIS_CREATE, false, 100)).thenReturn(80);
        when(selectionValidator.validateAndLoad(anyLong(), any()))
                .thenReturn(List.of(mock(SolveSession.class)));

        WeaknessEvidenceLogResponse wrong = evidence(9501L, "WRONG", "B");
        ConceptCandidate candidate = new ConceptCandidate(9301L, "완전 이진 트리", "한 줄 정의",
                5, 2, 1, 0, 40, new BigDecimal("0.800"), new BigDecimal("0.200"),
                WeaknessType.CONCEPT_CONFUSION, List.of(wrong));
        when(scoringService.score(any()))
                .thenReturn(new WeaknessScoringResult(List.of(candidate), 10, 2, 1, 0, true));
        when(reportRepository.findByRequestFingerprintAndStatus(anyString(), any())).thenReturn(Optional.empty());
        when(diagnosticProfileService.loadQuestionProfiles(any())).thenReturn(Map.of(
                9501L, profileView("RECALL", Map.of("B", "CPT_M01"))));

        // AI 호출이 실패한다(타임아웃 등).
        when(aiClient.analyze(any())).thenThrow(new RuntimeException("timeout"));

        CoreMateException ex = assertThrows(CoreMateException.class,
                () -> service.createReport(1L, request(), "key-1"));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WEAKNESS_ANALYSIS_FAILED);
        // 코인은 차감됐다가(준비 tx) 환불된다(보상 tx).
        verify(rewardService).spendCoin(eq(1L), eq(80), eq(CurrencyReason.WEAKNESS_ANALYSIS_CREATE), any(), any());
        verify(rewardService).earnCoin(eq(1L), eq(80), eq(CurrencyReason.REFUND), any(), any());
        // 같은 키로 재시도할 수 있도록 멱등키를 해제한다.
        verify(idempotencyGuard).unregister(1L, "key-1");
        // AI 실패 시 개념은 저장되지 않는다.
        verify(conceptRepository, never()).save(any());
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
        when(conceptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 두 오답이 같은 오개념 코드를 가리키고, LONG_TIME 문항은 코드가 없다.
        when(diagnosticProfileService.loadQuestionProfiles(any())).thenReturn(Map.of(
                9501L, profileView("RECALL", Map.of("B", "CPT_M01")),
                9502L, profileView("APPLICATION", Map.of("C", "CPT_M01")),
                9503L, profileView("ANALYSIS", Map.of())));
        when(diagnosticProfileService.loadConceptProfile(9301L)).thenReturn(Optional.of(
                new DiagnosticProfileService.ConceptProfileView("프로필 핵심", "프로필 설명",
                        List.of(), List.of(), List.of(), null, null, List.of())));
        when(aiClient.analyze(any())).thenReturn(new AiWeaknessAnalysisResponse("요약", List.of(), null));

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
        when(conceptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // AI 는 1번·3번 근거만 지목했다(e1, e3).
        when(aiClient.analyze(any())).thenReturn(new AiWeaknessAnalysisResponse("요약", List.of(
                new AiWeaknessAnalysisResponse.ConceptAnalysis(9401L, AiAnalysisStatus.SUPPORTED,
                        WeaknessType.CONCEPT_CONFUSION, List.of(), null, null, "진단", null, null,
                        List.of("e1", "e3"))), null));

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

    /**
     * 화면이 쓰는 값이 응답에 실리는지 본다: 원인 문장, 개념별 수치, 반복 오개념(제목 포함),
     * 근거 문제의 "그 선지를 고른 이유".
     */
    @Test
    void conceptResponseCarriesCauseCountsAndRepeatedMisconceptions() {
        when(props.getCoinCost()).thenReturn(100);
        when(props.getAiModelLabel()).thenReturn("model");
        when(selectionValidator.validateAndLoad(anyLong(), any()))
                .thenReturn(List.of(mock(SolveSession.class)));

        WeaknessEvidenceLogResponse wrongA = evidence(9501L, "WRONG", "B");
        WeaknessEvidenceLogResponse wrongB = evidence(9502L, "WRONG", "C");
        ConceptCandidate candidate = new ConceptCandidate(9301L, "완전 이진 트리", "한 줄 정의",
                3, 2, 0, 1, 60, new BigDecimal("0.800"), new BigDecimal("0.200"),
                WeaknessType.CONCEPT_CONFUSION, List.of(wrongA, wrongB));
        when(scoringService.score(any()))
                .thenReturn(new WeaknessScoringResult(List.of(candidate), 3, 2, 0, 1, true));
        when(reportRepository.findByRequestFingerprintAndStatus(anyString(), any()))
                .thenReturn(Optional.empty());
        when(conceptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        when(diagnosticProfileService.loadQuestionProfiles(any())).thenReturn(Map.of(
                9501L, new DiagnosticProfileService.QuestionProfileView(
                        "RECALL", Map.of("B", "CPT_M01"), Map.of("B", "B를 고른 이유")),
                9502L, new DiagnosticProfileService.QuestionProfileView(
                        "APPLICATION", Map.of("C", "CPT_M01"), Map.of("C", "C를 고른 이유"))));
        // 중첩 stubbing 이 되지 않도록 목을 먼저 만들고 넘긴다.
        CommonMistake mistake = commonMistake("CPT_M01", "완전 이진 트리와 포화 이진 트리를 같게 봄");
        when(commonMistakeRepository.findAllByMistakeCodeIn(any())).thenReturn(List.of(mistake));
        when(aiClient.analyze(any())).thenReturn(new AiWeaknessAnalysisResponse("요약",
                List.of(new AiWeaknessAnalysisResponse.ConceptAnalysis(9301L, AiAnalysisStatus.SUPPORTED,
                        WeaknessType.CONCEPT_CONFUSION, List.of(), CauseCode.REPEATED_MISCONCEPTION,
                        "혼동한 관계", "진단", "원인 문장", "복습 팁", List.of())),
                null));

        WeaknessAnalysisReportDetailResponse detail = service.createReport(1L, request());
        WeaknessConceptResponse concept = detail.weaknessConcepts().get(0);

        assertThat(concept.likelyCause()).isEqualTo("원인 문장");
        assertThat(concept.questionCount()).isEqualTo(3);
        assertThat(concept.wrongCount()).isEqualTo(2);
        assertThat(concept.hintUsedCount()).isEqualTo(1);
        assertThat(concept.repeatedMisconceptions()).hasSize(1);
        assertThat(concept.repeatedMisconceptions().get(0).code()).isEqualTo("CPT_M01");
        assertThat(concept.repeatedMisconceptions().get(0).title())
                .isEqualTo("완전 이진 트리와 포화 이진 트리를 같게 봄");
        assertThat(concept.repeatedMisconceptions().get(0).count()).isEqualTo(2);
        assertThat(concept.evidenceLogs()).extracting(WeaknessEvidenceLogResponse::selectedAnswerReason)
                .containsExactly("B를 고른 이유", "C를 고른 이유");
        assertThat(concept.evidenceLogs()).extracting(WeaknessEvidenceLogResponse::misconceptionTitle)
                .containsOnly("완전 이진 트리와 포화 이진 트리를 같게 봄");
    }

    /**
     * 개념 관계가 화면에 쓸 수 있는 형태로 응답에 실리는지 본다.
     * prerequisite 은 방향이 있어 target 쪽 개념에만 붙고, confusing 은 양쪽에 붙는다.
     */
    @Test
    void conceptResponseCarriesPrerequisiteAndConfusingRelations() {
        when(props.getCoinCost()).thenReturn(100);
        when(props.getAiModelLabel()).thenReturn("model");
        when(selectionValidator.validateAndLoad(anyLong(), any()))
                .thenReturn(List.of(mock(SolveSession.class)));

        ConceptCandidate bigO = new ConceptCandidate(9301L, "빅오 표기법", "핵심",
                2, 2, 0, 2, 70, new BigDecimal("0.800"), new BigDecimal("0.200"),
                WeaknessType.CONCEPT_CONFUSION, List.of());
        ConceptCandidate bigTheta = new ConceptCandidate(9302L, "빅세타 표기법", "핵심",
                3, 3, 0, 0, 50, new BigDecimal("0.700"), new BigDecimal("0.100"),
                WeaknessType.CONCEPT_CONFUSION, List.of());
        when(scoringService.score(any()))
                .thenReturn(new WeaknessScoringResult(List.of(bigO, bigTheta), 5, 5, 0, 2, true));
        when(reportRepository.findByRequestFingerprintAndStatus(anyString(), any()))
                .thenReturn(Optional.empty());
        when(conceptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(diagnosticProfileService.loadQuestionProfiles(any())).thenReturn(Map.of());
        when(aiClient.analyze(any())).thenReturn(new AiWeaknessAnalysisResponse("요약", List.of(), null));

        // 중첩 stubbing 이 되지 않도록 관계 목을 먼저 만들어 둔다.
        ConceptRelation prerequisite = relation("prerequisite", concept(9301L, "빅오 표기법"),
                concept(9302L, "빅세타 표기법"), "빅세타는 상한 개념인 빅오 이해를 바탕으로 더 잘 이해된다.");
        ConceptRelation confusing = relation("confusing", concept(9302L, "빅세타 표기법"),
                concept(9401L, "빅오메가 표기법"), "하한과 상하한 경계를 혼동하기 쉽다.");
        when(conceptRelationRepository.findActiveRelations(any(), any()))
                .thenReturn(List.of(prerequisite, confusing));

        WeaknessAnalysisReportDetailResponse detail = service.createReport(1L, request());

        assertThat(detail.weaknessConcepts().get(0).relatedConcepts()).isEmpty();

        List<RelatedConceptResponse> related = detail.weaknessConcepts().get(1).relatedConcepts();
        assertThat(related).extracting(RelatedConceptResponse::relationType)
                .containsExactly("PREREQUISITE", "CONFUSING");
        assertThat(related.get(0).conceptName()).isEqualTo("빅오 표기법");
        // 같은 분석표에서도 약점인 개념은 순위를 함께 준다(화면의 "1순위 약점" 표시용).
        assertThat(related.get(0).weaknessRank()).isEqualTo(1);
        assertThat(related.get(1).conceptName()).isEqualTo("빅오메가 표기법");
        assertThat(related.get(1).weaknessRank()).isNull();
    }

    private Concept concept(Long id, String name) {
        Concept concept = mock(Concept.class);
        when(concept.getConceptId()).thenReturn(id);
        // 관계의 반대쪽 개념 이름만 응답에 실려서, 한쪽 이름은 안 읽힌다(strict stubs 회피).
        lenient().when(concept.getConceptName()).thenReturn(name);
        return concept;
    }

    private ConceptRelation relation(String type, Concept source, Concept target, String note) {
        ConceptRelation relation = mock(ConceptRelation.class);
        when(relation.getRelationType()).thenReturn(type);
        when(relation.getSourceConcept()).thenReturn(source);
        when(relation.getTargetConcept()).thenReturn(target);
        when(relation.getRelationNote()).thenReturn(note);
        return relation;
    }

    private CommonMistake commonMistake(String code, String title) {
        CommonMistake mistake = mock(CommonMistake.class);
        when(mistake.getMistakeCode()).thenReturn(code);
        when(mistake.getMistakeTitle()).thenReturn(title);
        return mistake;
    }

    private WeaknessEvidenceLogResponse evidence(Long questionId, String type, String selectedAnswer) {
        return new WeaknessEvidenceLogResponse(null, questionId, questionId, "문제",
                type, 60, 60, 0, selectedAnswer, null, "A", null, null, null,
                !"WRONG".equals(type), false);
    }

    private DiagnosticProfileService.QuestionProfileView profileView(String skillType,
            Map<String, String> misconceptionByOption) {
        return new DiagnosticProfileService.QuestionProfileView(skillType, misconceptionByOption, Map.of());
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
