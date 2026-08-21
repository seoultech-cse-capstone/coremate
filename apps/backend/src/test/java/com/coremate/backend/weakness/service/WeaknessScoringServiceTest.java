package com.coremate.backend.weakness.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.coremate.backend.content.entity.Concept;
import com.coremate.backend.content.entity.Question;
import com.coremate.backend.solve.entity.SolveLog;
import com.coremate.backend.solve.entity.SolveSession;
import com.coremate.backend.solve.repository.SolveLogRepository;
import com.coremate.backend.weakness.config.WeaknessAnalysisProperties;
import com.coremate.backend.weakness.dto.response.WeaknessEvidenceLogResponse;
import com.coremate.backend.weakness.service.model.ConceptCandidate;
import com.coremate.backend.weakness.service.model.WeaknessScoringResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeaknessScoringServiceTest {

    @Mock
    private SolveLogRepository solveLogRepository;

    private WeaknessAnalysisProperties props;
    private WeaknessScoringService scoringService;

    @BeforeEach
    void setUp() {
        props = new WeaknessAnalysisProperties();
        scoringService = new WeaknessScoringService(solveLogRepository, props);
    }

    @Test
    void producesCandidateWithSignalsWhenSufficient() {
        props.setMinWeaknessScore(10);
        props.setMinEvidenceStrength(0.1);
        Concept concept = concept(100L, "완전 이진 트리");
        List<SolveLog> logs = List.of(
                log(concept, false, 30, 30),    // WRONG
                log(concept, false, 30, 30),    // WRONG
                log(concept, false, 30, 30),    // WRONG
                log(concept, true, 120, 30),    // LONG_TIME (correct, 4x expected)
                logHint(concept, true, 20, 30)  // HINT_USED
        );
        when(solveLogRepository.findAllBySolveSession_SolveSessionIdInOrderByQuestionOrder(any())).thenReturn(logs);

        WeaknessScoringResult result = scoringService.score(List.of(mockSession(1L)));

        assertThat(result.sufficient()).isTrue();
        assertThat(result.candidates()).hasSize(1);
        ConceptCandidate candidate = result.candidates().get(0);
        assertThat(candidate.conceptId()).isEqualTo(100L);
        assertThat(candidate.wrongCount()).isEqualTo(3);
        assertThat(candidate.longTimeCount()).isEqualTo(1);
        assertThat(candidate.hintUsedCount()).isEqualTo(1);
        assertThat(candidate.weaknessScore()).isGreaterThan(0);
        assertThat(candidate.evidenceStrength().doubleValue()).isGreaterThan(0.0);
        assertThat(candidate.backendCandidateType()).isNotNull();
        assertThat(candidate.evidence()).isNotEmpty();
        assertThat(result.totalWrongCount()).isEqualTo(3);
    }

    @Test
    void insufficientWhenBelowScoreThreshold() {
        // 기본 임계값(minWeaknessScore=60) + 약한 신호 → 후보 없음
        Concept concept = concept(100L, "완전 이진 트리");
        List<SolveLog> logs = List.of(
                log(concept, true, 20, 30),
                log(concept, true, 20, 30),
                log(concept, false, 20, 30),
                log(concept, true, 20, 30),
                log(concept, true, 20, 30)
        );
        when(solveLogRepository.findAllBySolveSession_SolveSessionIdInOrderByQuestionOrder(any())).thenReturn(logs);

        WeaknessScoringResult result = scoringService.score(List.of(mockSession(1L)));

        assertThat(result.sufficient()).isFalse();
        assertThat(result.candidates()).isEmpty();
    }

    @Test
    void insufficientWhenNoLogs() {
        when(solveLogRepository.findAllBySolveSession_SolveSessionIdInOrderByQuestionOrder(any())).thenReturn(List.of());

        WeaknessScoringResult result = scoringService.score(List.of(mockSession(1L)));

        assertThat(result.sufficient()).isFalse();
        assertThat(result.totalQuestionCount()).isZero();
    }

    @Test
    void 근거_유형은_오답_오래걸림_힌트_셋으로만_매겨진다() {
        props.setMinWeaknessScore(10);
        props.setMinEvidenceStrength(0.1);
        props.setMaxEvidencePerConcept(3);
        Concept concept = concept(100L, "완전 이진 트리");
        List<SolveLog> logs = List.of(
                log(concept, false, 30, 30),   // 오답
                log(concept, true, 120, 30),   // 맞았지만 오래 걸림
                logHint(concept, true, 20, 30) // 힌트 사용
        );
        when(solveLogRepository.findAllBySolveSession_SolveSessionIdInOrderByQuestionOrder(any()))
                .thenReturn(logs);

        ConceptCandidate candidate = scoringService.score(List.of(mockSession(1L))).candidates().get(0);

        // ANSWER_CHANGED 는 만들지 않는다. 답 변경은 근거가 아니라 회차별 집계로만 노출한다(FR-WEAK-03).
        assertThat(candidate.evidence())
                .extracting(WeaknessEvidenceLogResponse::evidenceType)
                .containsExactlyInAnyOrder("WRONG", "LONG_TIME", "HINT_USED")
                .doesNotContain("ANSWER_CHANGED");
    }

    private SolveSession mockSession(long id) {
        SolveSession session = mock(SolveSession.class);
        lenient().when(session.getSolveSessionId()).thenReturn(id);
        return session;
    }

    private Concept concept(long id, String name) {
        Concept concept = mock(Concept.class);
        lenient().when(concept.getConceptId()).thenReturn(id);
        lenient().when(concept.getConceptName()).thenReturn(name);
        lenient().when(concept.getOneLineDefinition()).thenReturn("한 줄 정의");
        return concept;
    }

    private SolveLog log(Concept concept, boolean correct, int timeSpent, Integer expected) {
        return buildLog(concept, correct, timeSpent, expected, false, 0);
    }

    private SolveLog logHint(Concept concept, boolean correct, int timeSpent, Integer expected) {
        return buildLog(concept, correct, timeSpent, expected, true, 2);
    }

    private SolveLog buildLog(Concept concept, boolean correct, int timeSpent, Integer expected,
            boolean hintUsed, int hintCount) {
        Question question = mock(Question.class);
        lenient().when(question.getConcept()).thenReturn(concept);
        lenient().when(question.getQuestionId()).thenReturn(500L);
        lenient().when(question.getQuestionText()).thenReturn("문제 본문");
        SolveLog log = mock(SolveLog.class);
        lenient().when(log.getQuestion()).thenReturn(question);
        lenient().when(log.isCorrect()).thenReturn(correct);
        lenient().when(log.getTimeSpentSec()).thenReturn(timeSpent);
        lenient().when(log.getExpectedSolveSecSnapshot()).thenReturn(expected);
        lenient().when(log.isHintUsed()).thenReturn(hintUsed);
        lenient().when(log.getHintCount()).thenReturn(hintCount);
        lenient().when(log.getSolveLogId()).thenReturn(1L);
        lenient().when(log.getSelectedAnswer()).thenReturn("A");
        lenient().when(log.getCorrectAnswerSnapshot()).thenReturn("B");
        return log;
    }
}
