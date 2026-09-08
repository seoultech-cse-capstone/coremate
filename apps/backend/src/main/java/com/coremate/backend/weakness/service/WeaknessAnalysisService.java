package com.coremate.backend.weakness.service;

import com.coremate.backend.content.entity.CategoryLv1;
import com.coremate.backend.content.entity.CategoryLv2;
import com.coremate.backend.content.entity.Concept;
import com.coremate.backend.content.entity.Subject;
import com.coremate.backend.content.repository.ConceptRepository;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.global.idempotency.IdempotencyGuard;
import com.coremate.backend.paid.service.FeatureAccessPricingService;
import com.coremate.backend.reward.entity.type.CurrencyReason;
import com.coremate.backend.reward.service.RewardService;
import com.coremate.backend.solve.entity.SolveLog;
import com.coremate.backend.solve.entity.SolveSession;
import com.coremate.backend.solve.repository.SolveLogRepository;
import com.coremate.backend.solve.repository.SolveSessionRepository;
import com.coremate.backend.weakness.client.WeaknessAiClient;
import com.coremate.backend.weakness.config.WeaknessAnalysisProperties;
import com.coremate.backend.weakness.dto.ai.AiWeaknessAnalysisRequest;
import com.coremate.backend.weakness.dto.ai.AiWeaknessAnalysisResponse;
import com.coremate.backend.weakness.dto.request.CreateWeaknessAnalysisReportRequest;
import com.coremate.backend.weakness.dto.response.EligibleSolveSessionResponse;
import com.coremate.backend.weakness.dto.response.WeaknessAnalysisReportDetailResponse;
import com.coremate.backend.weakness.dto.response.WeaknessAnalysisReportSummaryResponse;
import com.coremate.backend.weakness.dto.response.WeaknessConceptResponse;
import com.coremate.backend.weakness.dto.response.WeaknessEvidenceLogResponse;
import com.coremate.backend.weakness.dto.type.AiAnalysisStatus;
import com.coremate.backend.weakness.dto.type.AnalysisReportStatus;
import com.coremate.backend.weakness.dto.type.EvidenceType;
import com.coremate.backend.weakness.dto.type.FinalTypeSource;
import com.coremate.backend.weakness.dto.type.WeaknessType;
import com.coremate.backend.weakness.entity.WeaknessAnalysisAiCallLog;
import com.coremate.backend.weakness.entity.WeaknessAnalysisConcept;
import com.coremate.backend.weakness.entity.WeaknessAnalysisEvidenceLog;
import com.coremate.backend.weakness.entity.WeaknessAnalysisReport;
import com.coremate.backend.weakness.entity.WeaknessAnalysisReportViewLog;
import com.coremate.backend.weakness.entity.WeaknessAnalysisSession;
import com.coremate.backend.weakness.repository.WeaknessAnalysisAiCallLogRepository;
import com.coremate.backend.weakness.repository.WeaknessAnalysisConceptRepository;
import com.coremate.backend.weakness.repository.WeaknessAnalysisEvidenceLogRepository;
import com.coremate.backend.weakness.repository.WeaknessAnalysisReportRepository;
import com.coremate.backend.weakness.repository.WeaknessAnalysisReportViewLogRepository;
import com.coremate.backend.weakness.repository.WeaknessAnalysisSessionRepository;
import com.coremate.backend.weakness.service.model.ConceptCandidate;
import com.coremate.backend.weakness.service.model.WeaknessScoringResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * 약점분석 생성/조회 오케스트레이션(Phase 3).
 *
 * <p>생성: 검증 → 결정론 스코어링 → 근거부족 시 INSUFFICIENT_EVIDENCE 종료 → request_fingerprint 멱등
 * → 코인 차감 → AI 서버 호출 → 백엔드/AI 유형 교차검증 → 저장(report/sessions/concepts/evidence/ai_call_log).
 * 전 과정을 한 트랜잭션으로 묶어 AI 실패 시 코인 차감까지 롤백한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeaknessAnalysisService {

    private static final String REFERENCE_TYPE = "WEAKNESS_ANALYSIS_REPORT";

    /** AI 입력에 담을 반복 오개념 코드 최대 개수(문서 비용 절감 규칙: 상위 2). */
    private static final int MAX_REPEATED_MISCONCEPTIONS = 2;

    private final DiagnosticProfileService diagnosticProfileService;
    private final WeaknessSelectionValidator selectionValidator;
    private final WeaknessScoringService scoringService;
    private final WeaknessAiClient aiClient;
    private final RewardService rewardService;
    private final WeaknessAnalysisProperties props;
    private final FeatureAccessPricingService featureAccessPricingService;
    private final ObjectMapper objectMapper;
    private final WeaknessAnalysisReportRepository reportRepository;
    private final WeaknessAnalysisSessionRepository sessionRepository;
    private final WeaknessAnalysisConceptRepository conceptRepository;
    private final WeaknessAnalysisEvidenceLogRepository evidenceLogRepository;
    private final WeaknessAnalysisAiCallLogRepository aiCallLogRepository;
    private final WeaknessAnalysisReportViewLogRepository reportViewLogRepository;
    private final ConceptRepository contentConceptRepository;
    private final SolveLogRepository solveLogRepository;
    private final SolveSessionRepository weaknessSolveSessionRepository;
    private final IdempotencyGuard idempotencyGuard;

    // ----------------------------------------------------------------------- //
    // 생성
    // ----------------------------------------------------------------------- //
    // 컨트롤러 진입점. 멱등키를 먼저 등록해 동시/재시도 요청으로 비싼 LLM 호출·코인 차감·리포트가
    // 중복되지 않게 막고(#168), 실제 생성은 기존 createReport 를 같은 트랜잭션에서 재사용한다.
    @Transactional
    public WeaknessAnalysisReportDetailResponse createReport(
            Long userId, CreateWeaknessAnalysisReportRequest request, String idempotencyKey) {
        idempotencyGuard.register(userId, idempotencyKey, "WEAKNESS_REPORT_CREATE");
        return createReport(userId, request);
    }

    @Transactional
    public WeaknessAnalysisReportDetailResponse createReport(Long userId, CreateWeaknessAnalysisReportRequest request) {
        List<SolveSession> sessions = selectionValidator.validateAndLoad(userId, request.solveSessionIds());
        WeaknessScoringResult scoring = scoringService.score(sessions);
        if (!scoring.sufficient()) {
            throw new CoreMateException(ErrorCode.WEAKNESS_INSUFFICIENT_EVIDENCE);
        }

        String fingerprint = buildFingerprint(userId, request.solveSessionIds());
        // 멱등: 동일 요청으로 완료된 분석표가 있으면 재사용(코인·AI 호출 없음).
        WeaknessAnalysisReport existing = reportRepository
                .findByRequestFingerprintAndStatus(fingerprint, AnalysisReportStatus.COMPLETED)
                .orElse(null);
        if (existing != null) {
            return toDetail(existing);
        }

        Subject subject = resolveSubject(sessions.get(0));
        Long subjectId = subject != null ? subject.getSubjectId() : null;
        String scopeName = subject != null ? subject.getSubjectName() : null;

        int coinCost = getCreateCoinCost();

        // 리포트 저장(REQUESTED) 후 코인 차감(참조 연결). AI 실패 시 트랜잭션 롤백으로 코인 원복.
        WeaknessAnalysisReport report = reportRepository.save(WeaknessAnalysisReport.builder()
                .userId(userId)
                .subjectId(subjectId)
                .categoryLv1Id(request.categoryLv1Id())
                .categoryLv2Id(request.categoryLv2Id())
                .conceptId(request.conceptId())
                .analysisScopeType(request.analysisScopeType())
                .analysisScopeNameSnapshot(scopeName)
                .title(resolveTitle(request, scopeName))
                .reportFormat("MARKDOWN")
                .selectedSessionCount(sessions.size())
                .totalQuestionCount(scoring.totalQuestionCount())
                .totalWrongCount(scoring.totalWrongCount())
                .totalLongTimeCount(scoring.totalLongTimeCount())
                .totalHintUsedCount(scoring.totalHintUsedCount())
                .coinCost(coinCost)
                .requestFingerprint(fingerprint)
                .build());
        report.markGenerating();
        saveSessions(report, sessions);

        rewardService.spendCoin(userId, coinCost, CurrencyReason.WEAKNESS_ANALYSIS_CREATE,
                REFERENCE_TYPE, report.getAnalysisReportId());

        // AI 호출
        AiRequestBundle bundle = buildAiRequest(subject, scoring);
        long startedAt = System.currentTimeMillis();
        AiWeaknessAnalysisResponse aiResponse;
        try {
            aiResponse = aiClient.analyze(bundle.request());
        } catch (RuntimeException ex) {
            // 사용자에겐 동일한 메시지가 나가므로, 원인(타임아웃·서버다운·스키마 불일치 등)은
            // 여기서 남기지 않으면 사후에 구분할 방법이 없다.
            log.error("약점분석 AI 호출 실패 (userId={}, reportId={}, candidates={}, latencyMs={})",
                    userId, report.getAnalysisReportId(), scoring.candidates().size(),
                    System.currentTimeMillis() - startedAt, ex);
            throw new CoreMateException(ErrorCode.WEAKNESS_ANALYSIS_FAILED);
        }
        int latencyMs = (int) (System.currentTimeMillis() - startedAt);
        saveAiCallLog(report.getAnalysisReportId(), fingerprint, latencyMs, "SUCCESS", aiResponse.meta());

        // 교차검증 + 개념/근거 저장
        Map<Long, AiWeaknessAnalysisResponse.ConceptAnalysis> aiByConcept = indexAiByConcept(aiResponse);
        List<WeaknessConceptResponse> conceptResponses = new ArrayList<>();
        int sortOrder = 0;
        for (ConceptCandidate candidate : scoring.candidates()) {
            AiWeaknessAnalysisResponse.ConceptAnalysis ai = aiByConcept.get(candidate.conceptId());
            CrossResult cross = crossValidate(candidate.backendCandidateType(), ai, candidate.typeSeparation());

            WeaknessAnalysisConcept concept = conceptRepository.save(WeaknessAnalysisConcept.builder()
                    .report(report)
                    .conceptId(candidate.conceptId())
                    .sortOrder(sortOrder++)
                    .weaknessScore(candidate.weaknessScore())
                    .evidenceStrength(candidate.evidenceStrength())
                    .typeSeparation(candidate.typeSeparation())
                    .backendCandidateType(candidate.backendCandidateType())
                    .aiPrimaryType(cross.aiPrimaryType())
                    .aiSecondarySignals(writeSignals(ai))
                    .aiAnalysisStatus(cross.analysisStatus())
                    .typeAgreement(cross.agreement())
                    .finalType(cross.finalType())
                    .finalTypeSource(cross.finalTypeSource())
                    .reconciliationReason(cross.reason())
                    .causeCode(ai != null ? ai.causeCode() : null)
                    .confusedRelation(ai != null ? ai.confusedRelation() : null)
                    .diagnosis(ai != null ? ai.diagnosis() : null)
                    .likelyCause(ai != null ? ai.likelyCause() : null)
                    .conceptTip(ai != null ? ai.conceptTip() : null)
                    .build());

            List<WeaknessEvidenceLogResponse> evidence = markCitedEvidence(
                    candidate.evidence(),
                    bundle.evidenceRefsByConceptId().get(candidate.conceptId()),
                    ai != null ? ai.evidenceRefs() : null);
            saveEvidenceLogs(report.getAnalysisReportId(), concept.getAnalysisConceptId(), evidence);
            conceptResponses.add(toConceptResponse(concept, candidate, ai, cross, evidence));
        }

        report.markCompleted(aiResponse.overview(), null, props.getAiModelLabel(), props.getPromptVersion(),
                props.getSchemaVersion(), props.getScoringRuleVersion(), props.getDiagnosticProfileVersion());

        return buildDetail(report, sessions, conceptResponses);
    }

    // ----------------------------------------------------------------------- //
    // 조회 / 삭제
    // ----------------------------------------------------------------------- //
    @Transactional(readOnly = true)
    public List<WeaknessAnalysisReportSummaryResponse> getReports(Long userId) {
        List<WeaknessAnalysisReport> reports = reportRepository
                .findByUserIdAndStatusNotOrderByCreatedAtDesc(userId, AnalysisReportStatus.DELETED);
        if (reports.isEmpty()) {
            return List.of();
        }
        // 분석표마다 count 를 날리면 N+1 이라 한 번에 세어 맵으로 들고 간다.
        Map<Long, Long> conceptCounts = conceptRepository
                .countConceptsByReportIds(reports.stream().map(WeaknessAnalysisReport::getAnalysisReportId).toList())
                .stream()
                .collect(Collectors.toMap(
                        WeaknessAnalysisConceptRepository.ConceptCountByReport::getReportId,
                        WeaknessAnalysisConceptRepository.ConceptCountByReport::getConceptCount));
        return reports.stream()
                .map(report -> toSummary(report,
                        conceptCounts.getOrDefault(report.getAnalysisReportId(), 0L)))
                .toList();
    }

    @Transactional
    public WeaknessAnalysisReportDetailResponse getReportDetail(Long userId, Long analysisReportId) {
        WeaknessAnalysisReport report = reportRepository.findByAnalysisReportIdAndUserId(analysisReportId, userId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.WEAKNESS_REPORT_NOT_FOUND));
        if (report.getStatus() == AnalysisReportStatus.DELETED) {
            throw new CoreMateException(ErrorCode.WEAKNESS_REPORT_NOT_FOUND);
        }
        // 재열람은 무과금. 열람수/열람 로그만 기록.
        report.recordView();
        reportViewLogRepository.save(WeaknessAnalysisReportViewLog.builder()
                .analysisReportId(report.getAnalysisReportId())
                .userId(userId)
                .build());
        return toDetail(report);
    }

    @Transactional
    public WeaknessAnalysisReportSummaryResponse deleteReport(Long userId, Long analysisReportId) {
        WeaknessAnalysisReport report = reportRepository.findByAnalysisReportIdAndUserId(analysisReportId, userId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.WEAKNESS_REPORT_NOT_FOUND));
        report.softDelete();
        return toSummary(report);
    }

    // ----------------------------------------------------------------------- //
    // AI 요청 구성 + 교차검증
    // ----------------------------------------------------------------------- //
    private AiRequestBundle buildAiRequest(Subject subject, WeaknessScoringResult scoring) {
        String subjectName = subject != null ? subject.getSubjectName() : "";
        AiWeaknessAnalysisRequest.Totals totals = new AiWeaknessAnalysisRequest.Totals(
                scoring.totalQuestionCount(), scoring.totalWrongCount(),
                scoring.totalLongTimeCount(), scoring.totalHintUsedCount());

        // 문항 진단 프로필(스킬 유형·선택지별 오개념 코드)을 한 번에 조회해 근거에 결합한다.
        List<Long> questionIds = scoring.candidates().stream()
                .flatMap(c -> c.evidence().stream())
                .map(WeaknessEvidenceLogResponse::questionId)
                .filter(java.util.Objects::nonNull)
                .toList();
        Map<Long, DiagnosticProfileService.QuestionProfileView> questionProfiles =
                diagnosticProfileService.loadQuestionProfiles(questionIds);

        List<AiWeaknessAnalysisRequest.Candidate> candidates = new ArrayList<>();
        // AI 에 보낸 ref("e1", "e2"…)를 개념별·순서대로 기억해 둔다.
        // 응답의 evidence_refs 를 실제 근거 로그에 되돌려 붙이려면 이 매핑이 필요하다.
        Map<Long, List<String>> refsByConceptId = new LinkedHashMap<>();
        int refCounter = 1;
        for (ConceptCandidate candidate : scoring.candidates()) {
            List<AiWeaknessAnalysisRequest.Evidence> evidence = new ArrayList<>();
            Map<String, Integer> misconceptionCounts = new LinkedHashMap<>();
            List<String> refsInOrder = new ArrayList<>();
            for (WeaknessEvidenceLogResponse log : candidate.evidence()) {
                Double timeRatio = null;
                if ("LONG_TIME".equals(log.evidenceType()) && log.expectedSolveSec() != null
                        && log.expectedSolveSec() > 0 && log.timeSpentSec() != null) {
                    timeRatio = round2((double) log.timeSpentSec() / log.expectedSolveSec());
                }
                DiagnosticProfileService.QuestionProfileView profile = questionProfiles.get(log.questionId());
                String skillType = profile != null ? profile.skillType() : null;
                // 오개념 코드는 오답일 때만 의미가 있다(고른 선지가 어떤 오개념을 드러내는지).
                String misconceptionCode = null;
                if (profile != null && "WRONG".equals(log.evidenceType())) {
                    misconceptionCode = profile.misconceptionCodeFor(log.selectedAnswer());
                    if (misconceptionCode != null) {
                        misconceptionCounts.merge(misconceptionCode, 1, Integer::sum);
                    }
                }
                String ref = "e" + refCounter++;
                refsInOrder.add(ref);
                evidence.add(new AiWeaknessAnalysisRequest.Evidence(
                        ref, log.evidenceType(), skillType, misconceptionCode, timeRatio));
            }
            int q = candidate.questionCount();
            AiWeaknessAnalysisRequest.Stats stats = new AiWeaknessAnalysisRequest.Stats(
                    rate(candidate.wrongCount(), q), rate(candidate.longTimeCount(), q), rate(candidate.hintUsedCount(), q));
            String conceptCore = diagnosticProfileService.loadConceptProfile(candidate.conceptId())
                    .map(DiagnosticProfileService.ConceptProfileView::conceptCore)
                    .filter(core -> core != null && !core.isBlank())
                    .orElse(candidate.conceptCore());
            candidates.add(new AiWeaknessAnalysisRequest.Candidate(
                    candidate.conceptId(), candidate.conceptName(), conceptCore,
                    candidate.weaknessScore(), candidate.evidenceStrength().doubleValue(),
                    candidate.typeSeparation().doubleValue(), stats,
                    topRepeatedMisconceptions(misconceptionCounts), evidence));
            refsByConceptId.put(candidate.conceptId(), refsInOrder);
        }
        AiWeaknessAnalysisRequest request = new AiWeaknessAnalysisRequest(
                new AiWeaknessAnalysisRequest.Scope(subjectName), totals, candidates);
        return new AiRequestBundle(request, refsByConceptId);
    }

    /**
     * 반복 오개념 상위 {@value #MAX_REPEATED_MISCONCEPTIONS}개. 2회 이상 나타난 코드만 "반복"으로 본다.
     * (대표 근거 범위 안에서 집계하므로 개념당 최대 {@code maxEvidencePerConcept}건이 상한)
     */
    private List<AiWeaknessAnalysisRequest.RepeatedMisconception> topRepeatedMisconceptions(
            Map<String, Integer> counts) {
        return counts.entrySet().stream()
                .filter(e -> e.getValue() >= 2)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(MAX_REPEATED_MISCONCEPTIONS)
                .map(e -> new AiWeaknessAnalysisRequest.RepeatedMisconception(e.getKey(), e.getValue()))
                .toList();
    }

    private CrossResult crossValidate(WeaknessType backendType,
            AiWeaknessAnalysisResponse.ConceptAnalysis ai, BigDecimal typeSeparation) {
        if (ai == null) {
            return new CrossResult(null, null, FinalTypeSource.NONE, false,
                    AiAnalysisStatus.INSUFFICIENT, "AI 해석 없음");
        }
        AiAnalysisStatus status = ai.analysisStatus();
        WeaknessType aiType = ai.primaryType();
        boolean agreement = backendType == aiType;

        if (status == AiAnalysisStatus.INSUFFICIENT || status == AiAnalysisStatus.AMBIGUOUS) {
            return new CrossResult(aiType, null, FinalTypeSource.NONE, agreement, status, "AI 판정 " + status.name());
        }
        if (agreement) {
            return new CrossResult(aiType, aiType, FinalTypeSource.BOTH, true, status, "백엔드·AI 유형 일치");
        }
        double sep = typeSeparation != null ? typeSeparation.doubleValue() : 0.0;
        if (sep < props.getTypeSeparationThreshold()) {
            return new CrossResult(aiType, aiType, FinalTypeSource.AI, false, status, "유형 구분도 낮아 AI 유형 채택");
        }
        return new CrossResult(aiType, null, FinalTypeSource.NONE, false, status, "유형 불일치·구분도 높아 판단 보류");
    }

    // ----------------------------------------------------------------------- //
    // 저장 헬퍼
    // ----------------------------------------------------------------------- //
    private void saveSessions(WeaknessAnalysisReport report, List<SolveSession> sessions) {
        int order = 0;
        for (SolveSession session : sessions) {
            sessionRepository.save(WeaknessAnalysisSession.builder()
                    .report(report)
                    .solveSessionId(session.getSolveSessionId())
                    .selectedOrder(order++)
                    .solveSessionTitleSnapshot(session.getTitle())
                    .wrongCountSnapshot(session.getWrongCount())
                    .build());
        }
    }

    /**
     * AI 응답의 {@code evidence_refs} 를 실제 근거 목록에 되돌려 붙인다.
     *
     * <p>ref 는 요청을 만들 때 근거 순서대로 매긴 값이라, 같은 순서로 짝지으면 어떤 근거를 지목한
     * 것인지 알 수 있다. AI 가 없거나(실패) 지목이 없으면 전부 false 로 둔다.
     */
    private List<WeaknessEvidenceLogResponse> markCitedEvidence(List<WeaknessEvidenceLogResponse> evidence,
            List<String> refsInOrder, List<String> citedRefs) {
        if (citedRefs == null || citedRefs.isEmpty() || refsInOrder == null) {
            return evidence;
        }
        Set<String> cited = Set.copyOf(citedRefs);
        List<WeaknessEvidenceLogResponse> marked = new ArrayList<>(evidence.size());
        for (int i = 0; i < evidence.size(); i++) {
            boolean isCited = i < refsInOrder.size() && cited.contains(refsInOrder.get(i));
            WeaknessEvidenceLogResponse log = evidence.get(i);
            marked.add(isCited ? withCited(log) : log);
        }
        return marked;
    }

    private WeaknessEvidenceLogResponse withCited(WeaknessEvidenceLogResponse log) {
        return new WeaknessEvidenceLogResponse(log.evidenceId(), log.solveLogId(), log.questionId(),
                log.questionText(), log.evidenceType(), log.timeSpentSec(), log.expectedSolveSec(),
                log.hintCount(), log.selectedAnswer(), log.correctAnswer(), log.isCorrect(), true);
    }

    private void saveEvidenceLogs(Long reportId, Long conceptId, List<WeaknessEvidenceLogResponse> evidence) {
        for (WeaknessEvidenceLogResponse log : evidence) {
            evidenceLogRepository.save(WeaknessAnalysisEvidenceLog.builder()
                    .analysisReportId(reportId)
                    .analysisConceptId(conceptId)
                    .solveLogId(log.solveLogId())
                    .evidenceType(EvidenceType.valueOf(log.evidenceType()))
                    .citedByAi(Boolean.TRUE.equals(log.citedByAi()))
                    .build());
        }
    }

    /**
     * AI 호출 1건을 기록한다. 모델명·토큰 수는 AI 서버가 실제 호출에서 받은 값을 그대로 적재한다
     * ({@code meta}). 프로바이더가 사용량을 주지 않으면 해당 컬럼은 null 로 남고, 모델명만
     * 비어 있을 때 설정값 라벨로 대체한다 — 어느 모델로 얼마를 썼는지 사후에 따질 수 있어야 한다.
     */
    private void saveAiCallLog(Long reportId, String fingerprint, int latencyMs, String status,
            AiWeaknessAnalysisResponse.CallMeta meta) {
        aiCallLogRepository.save(WeaknessAnalysisAiCallLog.builder()
                .analysisReportId(reportId)
                .requestFingerprint(fingerprint)
                .model(meta != null && meta.model() != null ? meta.model() : props.getAiModelLabel())
                .promptVersion(props.getPromptVersion())
                .schemaVersion(props.getSchemaVersion())
                .scoringRuleVersion(props.getScoringRuleVersion())
                .diagnosticProfileVersion(props.getDiagnosticProfileVersion())
                .inputTokens(meta != null ? meta.inputTokens() : null)
                .cachedTokens(meta != null ? meta.cachedTokens() : null)
                .outputTokens(meta != null ? meta.outputTokens() : null)
                .reasoningTokens(meta != null ? meta.reasoningTokens() : null)
                .retryCount(meta != null && meta.retryCount() != null ? meta.retryCount() : 0)
                .escalationCount(0)
                .latencyMs(latencyMs)
                .status(status)
                .build());
    }

    // ----------------------------------------------------------------------- //
    // 응답 매핑
    // ----------------------------------------------------------------------- //
    private WeaknessConceptResponse toConceptResponse(WeaknessAnalysisConcept concept, ConceptCandidate candidate,
            AiWeaknessAnalysisResponse.ConceptAnalysis ai, CrossResult cross,
            List<WeaknessEvidenceLogResponse> evidence) {
        String evidenceSummary = String.format("오답 %d개, 오래 걸린 문제 %d개, 힌트 사용 %d개",
                candidate.wrongCount(), candidate.longTimeCount(), candidate.hintUsedCount());
        DiagnosticProfileService.ConceptProfileView profile =
                diagnosticProfileService.loadConceptProfile(candidate.conceptId()).orElse(null);
        return new WeaknessConceptResponse(
                concept.getAnalysisConceptId(),
                candidate.conceptId(),
                candidate.conceptName(),
                candidate.weaknessScore(),
                candidate.evidenceStrength(),
                cross.finalType() != null ? cross.finalType().name() : null,
                cross.finalTypeSource().name(),
                ai != null ? ai.secondarySignals().stream().map(Enum::name).toList() : List.of(),
                ai != null && ai.causeCode() != null ? ai.causeCode().name() : null,
                cross.analysisStatus().name(),
                ai != null ? ai.confusedRelation() : null,
                ai != null ? ai.diagnosis() : null,
                evidenceSummary,
                detailedExplanation(profile, candidate.conceptCore()),
                ai != null ? ai.conceptTip() : null,
                profile != null ? profile.toResponse() : null,
                evidence);
    }

    /** 상세 설명은 개념 진단 프로필의 설명을 우선 사용하고, 없으면 개념 한 줄 정의로 대체한다. */
    private String detailedExplanation(DiagnosticProfileService.ConceptProfileView profile, String fallback) {
        if (profile != null && profile.conceptExplanation() != null && !profile.conceptExplanation().isBlank()) {
            return profile.conceptExplanation();
        }
        return fallback;
    }

    /** 저장된 리포트로부터 상세 응답을 재구성(재열람용). */
    private WeaknessAnalysisReportDetailResponse toDetail(WeaknessAnalysisReport report) {
        // 근거 로그는 solve_log_id 만 저장하므로, 문제 본문·답안·소요 시간은 풀이 로그에서 다시 읽는다.
        // 개념마다 따로 읽으면 N+1 이 되어 리포트 단위로 한 번에 모아 둔다.
        List<WeaknessAnalysisEvidenceLog> evidenceLogs =
                evidenceLogRepository.findByAnalysisReportId(report.getAnalysisReportId());
        Map<Long, SolveLog> solveLogsById = loadSolveLogs(evidenceLogs);

        List<WeaknessConceptResponse> concepts = conceptRepository
                .findByReport_AnalysisReportIdOrderBySortOrderAsc(report.getAnalysisReportId())
                .stream()
                .map(concept -> toConceptResponse(concept, evidenceLogs, solveLogsById))
                .toList();
        // 회차 스냅샷에는 제목과 오답 수만 있어서, 전체 문항 수는 원본 회차에서 다시 읽는다.
        // 없으면 화면이 "13/ 오답"처럼 분모가 빈 채로 그려진다.
        List<WeaknessAnalysisSession> selected = sessionRepository
                .findByReport_AnalysisReportIdOrderBySelectedOrderAsc(report.getAnalysisReportId());
        Map<Long, SolveSession> solveSessionsById = loadSolveSessions(selected);
        List<EligibleSolveSessionResponse> sessions = selected.stream()
                .map(session -> toSelectedSession(session, solveSessionsById.get(session.getSolveSessionId())))
                .toList();
        return buildDetailResponse(report, sessions, concepts);
    }

    /** 선택된 회차의 원본을 한 번에 읽어 id 로 찾을 수 있게 만든다. */
    private Map<Long, SolveSession> loadSolveSessions(List<WeaknessAnalysisSession> selected) {
        List<Long> solveSessionIds = selected.stream()
                .map(WeaknessAnalysisSession::getSolveSessionId)
                .toList();
        if (solveSessionIds.isEmpty()) {
            return Map.of();
        }
        return weaknessSolveSessionRepository.findAllById(solveSessionIds).stream()
                .collect(Collectors.toMap(SolveSession::getSolveSessionId, session -> session));
    }

    /** 근거 로그가 가리키는 풀이 로그를 한 번에 읽어 id 로 찾을 수 있게 만든다. */
    private Map<Long, SolveLog> loadSolveLogs(List<WeaknessAnalysisEvidenceLog> evidenceLogs) {
        Set<Long> solveLogIds = evidenceLogs.stream()
                .map(WeaknessAnalysisEvidenceLog::getSolveLogId)
                .collect(Collectors.toSet());
        if (solveLogIds.isEmpty()) {
            return Map.of();
        }
        return solveLogRepository.findAllWithQuestionBySolveLogIdIn(solveLogIds).stream()
                .collect(Collectors.toMap(SolveLog::getSolveLogId, log -> log));
    }

    private WeaknessConceptResponse toConceptResponse(WeaknessAnalysisConcept concept,
            List<WeaknessAnalysisEvidenceLog> evidenceLogs, Map<Long, SolveLog> solveLogsById) {
        List<WeaknessEvidenceLogResponse> evidence = evidenceLogs.stream()
                .filter(e -> concept.getAnalysisConceptId().equals(e.getAnalysisConceptId()))
                .map(e -> toEvidenceResponse(e, solveLogsById.get(e.getSolveLogId())))
                .toList();
        Concept contentConcept = contentConceptRepository.findById(concept.getConceptId()).orElse(null);
        String conceptName = contentConcept != null ? contentConcept.getConceptName() : null;
        // 재열람 시에도 개념 진단 프로필을 다시 조회해 학습 콘텐츠를 채운다(코인 재차감 없음).
        DiagnosticProfileService.ConceptProfileView profile =
                diagnosticProfileService.loadConceptProfile(concept.getConceptId()).orElse(null);
        String fallbackCore = contentConcept != null ? contentConcept.getOneLineDefinition() : null;
        return new WeaknessConceptResponse(
                concept.getAnalysisConceptId(),
                concept.getConceptId(),
                conceptName,
                concept.getWeaknessScore(),
                concept.getEvidenceStrength(),
                concept.getFinalType() != null ? concept.getFinalType().name() : null,
                concept.getFinalTypeSource() != null ? concept.getFinalTypeSource().name() : null,
                readSignals(concept.getAiSecondarySignals()),
                concept.getCauseCode() != null ? concept.getCauseCode().name() : null,
                concept.getAiAnalysisStatus() != null ? concept.getAiAnalysisStatus().name() : null,
                concept.getConfusedRelation(),
                concept.getDiagnosis(),
                null,
                detailedExplanation(profile, fallbackCore),
                concept.getConceptTip(),
                profile != null ? profile.toResponse() : null,
                evidence);
    }

    /**
     * 저장된 근거 로그 한 건을 응답으로 바꾼다.
     * 풀이 로그가 남아 있으면 생성 직후 응답과 같은 내용을 보여 주고, 없으면 최소 정보만 남긴다.
     */
    private WeaknessEvidenceLogResponse toEvidenceResponse(WeaknessAnalysisEvidenceLog evidence, SolveLog log) {
        if (log == null) {
            return new WeaknessEvidenceLogResponse(evidence.getEvidenceLogId(), evidence.getSolveLogId(),
                    null, null, evidence.getEvidenceType().name(), null, null, null, null, null, null,
                    evidence.isCitedByAi());
        }
        return new WeaknessEvidenceLogResponse(
                evidence.getEvidenceLogId(),
                evidence.getSolveLogId(),
                log.getQuestion().getQuestionId(),
                log.getQuestion().getQuestionText(),
                evidence.getEvidenceType().name(),
                log.getTimeSpentSec(),
                log.getExpectedSolveSecSnapshot(),
                log.getHintCount(),
                log.getSelectedAnswer(),
                log.getCorrectAnswerSnapshot(),
                log.isCorrect(),
                evidence.isCitedByAi());
    }

    private EligibleSolveSessionResponse toSelectedSession(WeaknessAnalysisSession session, SolveSession source) {
        Integer totalQuestionCount = source != null ? source.getTotalQuestionCount() : null;
        return new EligibleSolveSessionResponse(
                session.getSolveSessionId(), session.getSolveSessionTitleSnapshot(), null,
                null, null, null, null, null, null, null, null,
                totalQuestionCount, session.getWrongCountSnapshot(), null, null, null, null, true, null);
    }

    private WeaknessAnalysisReportDetailResponse buildDetail(WeaknessAnalysisReport report,
            List<SolveSession> sessions, List<WeaknessConceptResponse> concepts) {
        List<EligibleSolveSessionResponse> selected = sessions.stream()
                .map(this::toSelectedSession)
                .toList();
        return buildDetailResponse(report, selected, concepts);
    }

    private WeaknessAnalysisReportDetailResponse buildDetailResponse(WeaknessAnalysisReport report,
            List<EligibleSolveSessionResponse> sessions, List<WeaknessConceptResponse> concepts) {
        return new WeaknessAnalysisReportDetailResponse(
                report.getAnalysisReportId(),
                report.getUserId(),
                report.getTitle(),
                report.getAnalysisScopeType(),
                report.getAnalysisScopeNameSnapshot(),
                report.getSummaryText(),
                report.getAnalysisText(),
                report.getReportFormat(),
                report.getSelectedSessionCount(),
                report.getTotalQuestionCount(),
                report.getTotalWrongCount(),
                report.getTotalLongTimeCount(),
                report.getTotalHintUsedCount(),
                report.getCoinCost(),
                report.getStatus(),
                report.getRequestedAt(),
                report.getGeneratedAt(),
                sessions,
                concepts);
    }

    private EligibleSolveSessionResponse toSelectedSession(SolveSession session) {
        Subject subject = resolveSubject(session);
        CategoryLv1 lv1 = session.getCategoryLv1();
        CategoryLv2 lv2 = session.getCategoryLv2();
        return new EligibleSolveSessionResponse(
                session.getSolveSessionId(), session.getTitle(), session.getSessionType(),
                subject != null ? subject.getSubjectId() : null, subject != null ? subject.getSubjectName() : null,
                lv1 != null ? lv1.getCategoryLv1Id() : null, lv1 != null ? lv1.getCategoryLv1Name() : null,
                lv2 != null ? lv2.getCategoryLv2Id() : null, lv2 != null ? lv2.getCategoryLv2Name() : null,
                null, null,
                session.getTotalQuestionCount(), session.getWrongCount(), null, null, null,
                session.getCompletedAt(), true, null);
    }

    private WeaknessAnalysisReportSummaryResponse toSummary(WeaknessAnalysisReport report) {
        return toSummary(report, conceptRepository.countByReport_AnalysisReportId(report.getAnalysisReportId()));
    }

    private WeaknessAnalysisReportSummaryResponse toSummary(WeaknessAnalysisReport report, long weaknessConceptCount) {
        return new WeaknessAnalysisReportSummaryResponse(
                report.getAnalysisReportId(),
                report.getTitle(),
                report.getAnalysisScopeType(),
                report.getAnalysisScopeNameSnapshot(),
                report.getStatus(),
                report.getSelectedSessionCount(),
                report.getTotalQuestionCount(),
                report.getTotalWrongCount(),
                (int) weaknessConceptCount,
                report.getCoinCost(),
                report.getGeneratedAt(),
                false);
    }

    // ----------------------------------------------------------------------- //
    // 유틸
    // ----------------------------------------------------------------------- //
    private Map<Long, AiWeaknessAnalysisResponse.ConceptAnalysis> indexAiByConcept(AiWeaknessAnalysisResponse response) {
        Map<Long, AiWeaknessAnalysisResponse.ConceptAnalysis> map = new LinkedHashMap<>();
        if (response.conceptAnalyses() != null) {
            for (AiWeaknessAnalysisResponse.ConceptAnalysis analysis : response.conceptAnalyses()) {
                if (analysis.conceptId() != null) {
                    map.put(analysis.conceptId(), analysis);
                }
            }
        }
        return map;
    }

    private String writeSignals(AiWeaknessAnalysisResponse.ConceptAnalysis ai) {
        if (ai == null || ai.secondarySignals() == null) {
            return null;
        }
        return objectMapper.writeValueAsString(ai.secondarySignals().stream().map(Enum::name).toList());
    }

    private List<String> readSignals(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return objectMapper.readValue(json, new tools.jackson.core.type.TypeReference<List<String>>() {});
    }

    /**
     * 분석표 1건 생성에 필요한 코인. 생성 비용은 feature_access_products 정책에서 읽고,
     * 정책 행이 없으면 weakness.analysis.coin-cost 설정값으로 떨어진다(시드 미적용 환경 대비).
     *
     * <p>생성 요청과 사전 조회(SCR-WEAK-03 "필요한 코인")가 이 메서드를 함께 쓰므로 화면에
     * 안내한 값과 실제 차감액이 어긋나지 않는다.
     *
     * <p>프리미엄 구독 판정은 구독 도메인(user_subscriptions)이 없어 아직 붙이지 못한다.
     * 그래서 FR-PAY-01의 "프리미엄 50코인"이 적용되지 않고 전원 무과금 가격을 낸다.
     */
    @Transactional(readOnly = true)
    public int getCreateCoinCost() {
        return featureAccessPricingService.priceCoinOf(
                FeatureAccessPricingService.WEAKNESS_ANALYSIS_CREATE, false, props.getCoinCost());
    }

    private String buildFingerprint(Long userId, List<Long> sessionIds) {
        String raw = userId + "|" + sessionIds.stream().sorted().toList()
                + "|" + props.getScoringRuleVersion() + "|" + props.getDiagnosticProfileVersion()
                + "|" + props.getPromptVersion() + "|" + props.getSchemaVersion();
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new CoreMateException(ErrorCode.WEAKNESS_ANALYSIS_FAILED);
        }
    }

    private String resolveTitle(CreateWeaknessAnalysisReportRequest request, String scopeName) {
        if (request.title() != null && !request.title().isBlank()) {
            return request.title();
        }
        return (scopeName != null ? scopeName + " " : "") + "약점 분석표";
    }

    private Subject resolveSubject(SolveSession session) {
        if (session.getSubject() != null) {
            return session.getSubject();
        }
        if (session.getCategoryLv2() != null) {
            return session.getCategoryLv2().getCategoryLv1().getSubject();
        }
        if (session.getCategoryLv1() != null) {
            return session.getCategoryLv1().getSubject();
        }
        return null;
    }

    private double rate(int count, int total) {
        return total == 0 ? 0.0 : round2((double) count / total);
    }

    private double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private record AiRequestBundle(AiWeaknessAnalysisRequest request,
            Map<Long, List<String>> evidenceRefsByConceptId) {
    }

    private record CrossResult(WeaknessType aiPrimaryType, WeaknessType finalType, FinalTypeSource finalTypeSource,
            boolean agreement, AiAnalysisStatus analysisStatus, String reason) {
    }
}
