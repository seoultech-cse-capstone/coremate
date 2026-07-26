package com.coremate.backend.weakness.service;

import com.coremate.backend.content.entity.Concept;
import com.coremate.backend.solve.entity.SolveLog;
import com.coremate.backend.solve.entity.SolveSession;
import com.coremate.backend.solve.repository.SolveLogRepository;
import com.coremate.backend.weakness.config.WeaknessAnalysisProperties;
import com.coremate.backend.weakness.dto.response.WeaknessEvidenceLogResponse;
import com.coremate.backend.weakness.dto.type.WeaknessType;
import com.coremate.backend.weakness.service.model.ConceptCandidate;
import com.coremate.backend.weakness.service.model.WeaknessScoringResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결정론 스코어링: 선택된 회차의 풀이 로그를 개념별로 집계해 약점 후보·약점 점수·근거 충분도·
 * 백엔드 유형 후보를 산출한다. (수정안 1.2-B/C의 백엔드 사전 분석 파트)
 *
 * <p>수식은 문서에 확정값이 없어 플레이스홀더이며 {@link WeaknessAnalysisProperties}로 튜닝한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeaknessScoringService {

    private final SolveLogRepository solveLogRepository;
    private final WeaknessAnalysisProperties props;

    public WeaknessScoringResult score(List<SolveSession> sessions) {
        List<Long> sessionIds = sessions.stream().map(SolveSession::getSolveSessionId).toList();
        List<SolveLog> logs = solveLogRepository.findAllBySolveSession_SolveSessionIdInOrderByQuestionOrder(sessionIds);

        Map<Long, List<SolveLog>> byConcept = logs.stream()
                .filter(l -> l.getQuestion() != null && l.getQuestion().getConcept() != null)
                .collect(Collectors.groupingBy(
                        l -> l.getQuestion().getConcept().getConceptId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<ConceptCandidate> candidates = byConcept.values().stream()
                .map(this::buildCandidate)
                .filter(c -> c.weaknessScore() >= props.getMinWeaknessScore()
                        && c.evidenceStrength().doubleValue() >= props.getMinEvidenceStrength())
                .sorted(Comparator.comparingInt(ConceptCandidate::weaknessScore).reversed())
                .limit(props.getMaxCandidates())
                .toList();

        int totalQuestionCount = logs.size();
        int totalWrongCount = (int) logs.stream().filter(l -> !l.isCorrect()).count();
        int totalLongTimeCount = (int) logs.stream().filter(this::isLongTime).count();
        int totalHintUsedCount = (int) logs.stream().filter(SolveLog::isHintUsed).count();

        boolean sufficient = !candidates.isEmpty() && totalQuestionCount >= props.getMinQuestionCount();

        return new WeaknessScoringResult(
                candidates, totalQuestionCount, totalWrongCount, totalLongTimeCount, totalHintUsedCount, sufficient);
    }

    private ConceptCandidate buildCandidate(List<SolveLog> logs) {
        Concept concept = logs.get(0).getQuestion().getConcept();
        int questionCount = logs.size();
        int wrongCount = (int) logs.stream().filter(l -> !l.isCorrect()).count();
        int longTimeCount = (int) logs.stream().filter(this::isLongTime).count();
        int hintUsedCount = (int) logs.stream().filter(SolveLog::isHintUsed).count();

        double wrongRate = questionCount == 0 ? 0 : (double) wrongCount / questionCount;
        double longRate = questionCount == 0 ? 0 : (double) longTimeCount / questionCount;
        double hintRate = questionCount == 0 ? 0 : (double) hintUsedCount / questionCount;

        int weaknessScore = (int) Math.round(100 * (0.5 * wrongRate + 0.3 * longRate + 0.2 * hintRate));

        double strengthRaw = (double) (wrongCount * 2 + longTimeCount + hintUsedCount) / (questionCount + 3);
        BigDecimal evidenceStrength = BigDecimal.valueOf(Math.min(1.0, strengthRaw)).setScale(3, RoundingMode.HALF_UP);

        WeaknessType backendCandidateType = classify(wrongCount, longTimeCount, hintUsedCount);
        List<WeaknessEvidenceLogResponse> evidence = pickEvidence(logs);

        return new ConceptCandidate(
                concept.getConceptId(), concept.getConceptName(),
                questionCount, wrongCount, longTimeCount, hintUsedCount,
                weaknessScore, evidenceStrength, backendCandidateType, evidence);
    }

    /** 백엔드 내부 유형 후보(플레이스홀더 휴리스틱). AI 교차검증용이며 사용자 표시값 아님. */
    private WeaknessType classify(int wrongCount, int longTimeCount, int hintUsedCount) {
        if (longTimeCount > 0 && longTimeCount >= wrongCount && longTimeCount >= hintUsedCount) {
            return WeaknessType.SPEED_FLUENCY;
        }
        if (hintUsedCount > 0 && hintUsedCount >= wrongCount) {
            return WeaknessType.APPLICATION_GAP;
        }
        return WeaknessType.CONCEPT_CONFUSION;
    }

    /** 대표 근거: 오답 → 오래걸림 → 힌트 순, 개념당 최대 개수만큼. */
    private List<WeaknessEvidenceLogResponse> pickEvidence(List<SolveLog> logs) {
        List<SolveLog> ordered = new ArrayList<>();
        logs.stream().filter(l -> !l.isCorrect()).forEach(ordered::add);
        logs.stream().filter(this::isLongTime).forEach(ordered::add);
        logs.stream().filter(SolveLog::isHintUsed).forEach(ordered::add);
        return ordered.stream()
                .distinct()
                .limit(props.getMaxEvidencePerConcept())
                .map(this::toEvidence)
                .toList();
    }

    private WeaknessEvidenceLogResponse toEvidence(SolveLog log) {
        String evidenceType = !log.isCorrect() ? "WRONG"
                : isLongTime(log) ? "LONG_TIME"
                : log.isHintUsed() ? "HINT_USED"
                : "ANSWER_CHANGED";
        return new WeaknessEvidenceLogResponse(
                null,
                log.getSolveLogId(),
                log.getQuestion().getQuestionId(),
                log.getQuestion().getQuestionText(),
                evidenceType,
                log.getTimeSpentSec(),
                log.getExpectedSolveSecSnapshot(),
                log.getHintCount(),
                log.getSelectedAnswer(),
                log.getCorrectAnswerSnapshot(),
                log.isCorrect());
    }

    private boolean isLongTime(SolveLog log) {
        return log.isCorrect()
                && log.getExpectedSolveSecSnapshot() != null
                && log.getExpectedSolveSecSnapshot() > 0
                && log.getTimeSpentSec() >= log.getExpectedSolveSecSnapshot() * props.getLongTimeRatio();
    }
}
