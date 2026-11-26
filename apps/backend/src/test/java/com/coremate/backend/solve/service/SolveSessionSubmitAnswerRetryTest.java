package com.coremate.backend.solve.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coremate.backend.content.entity.Question;
import com.coremate.backend.content.repository.CategoryLv1Repository;
import com.coremate.backend.content.repository.CategoryLv2Repository;
import com.coremate.backend.content.repository.ConceptRepository;
import com.coremate.backend.content.repository.QuestionRepository;
import com.coremate.backend.content.repository.SubjectRepository;
import com.coremate.backend.global.idempotency.IdempotencyGuard;
import com.coremate.backend.learninghistory.service.LearningHistoryService;
import com.coremate.backend.progress.repository.UserConceptProgressRepository;
import com.coremate.backend.solve.dto.request.SubmitAnswerRequest;
import com.coremate.backend.solve.dto.response.SubmitAnswerResponse;
import com.coremate.backend.solve.dto.type.SolveSessionStatus;
import com.coremate.backend.solve.dto.type.SolveSessionType;
import com.coremate.backend.solve.entity.SolveLog;
import com.coremate.backend.solve.entity.SolveSession;
import com.coremate.backend.solve.repository.SolveLogRepository;
import com.coremate.backend.solve.repository.SolveSessionRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

/**
 * 앱이 답안 제출 응답을 못 받고 같은 문제를 다시 보낸 경우를 검증한다.
 *
 * <p>서버가 느려 앱이 기다리다 끊으면 서버에는 답이 이미 저장돼 있다. 예전에는 재제출을
 * ANSWER_ALREADY_SUBMITTED 로 거절해서 앱이 채점 결과를 받지 못하고 풀이가 끊겼다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("답안 재제출")
class SolveSessionSubmitAnswerRetryTest {

    private static final Long USER_ID = 7L;
    private static final Long SESSION_ID = 300L;
    private static final Long QUESTION_ID = 1002L;

    @Mock private SubjectRepository subjectRepository;
    @Mock private CategoryLv1Repository categoryLv1Repository;
    @Mock private CategoryLv2Repository categoryLv2Repository;
    @Mock private ConceptRepository conceptRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private SolveSessionRepository solveSessionRepository;
    @Mock private SolveLogRepository solveLogRepository;
    @Mock private UserConceptProgressRepository userConceptProgressRepository;
    @Mock private LearningHistoryService learningHistoryService;
    @Mock private SolveRewardService solveRewardService;
    @Mock private SolveSessionExpiryService solveSessionExpiryService;
    @Mock private IdempotencyGuard idempotencyGuard;

    private SolveSessionService solveSessionService;

    @BeforeEach
    void setUp() {
        solveSessionService = new SolveSessionService(
                subjectRepository,
                categoryLv1Repository,
                categoryLv2Repository,
                conceptRepository,
                questionRepository,
                solveSessionRepository,
                solveLogRepository,
                userConceptProgressRepository,
                learningHistoryService,
                solveRewardService,
                solveSessionExpiryService,
                new ObjectMapper(),
                idempotencyGuard);
    }

    @Test
    @DisplayName("이미 저장된 문제를 다시 보내면 새로 저장하지 않고 처음 저장한 채점 결과를 돌려준다")
    void 재제출은_처음_저장한_결과를_돌려준다() {
        SolveSession session = mock(SolveSession.class);
        when(session.getStatus()).thenReturn(SolveSessionStatus.IN_PROGRESS);
        when(session.getSessionType()).thenReturn(SolveSessionType.CONCEPT_PRACTICE);
        when(session.getTotalQuestionCount()).thenReturn(7);
        when(solveSessionRepository.findBySolveSessionIdAndUserId(SESSION_ID, USER_ID))
                .thenReturn(Optional.of(session));

        Question question = mock(Question.class);
        when(question.getQuestionId()).thenReturn(QUESTION_ID);
        when(question.getExplanationText()).thenReturn("해설");
        SolveLog saved = mock(SolveLog.class);
        when(saved.getSolveLogId()).thenReturn(55L);
        when(saved.getQuestion()).thenReturn(question);
        when(saved.getQuestionOrder()).thenReturn(2);
        when(saved.getSelectedAnswer()).thenReturn("C");
        when(saved.getCorrectAnswerSnapshot()).thenReturn("B");
        when(saved.isCorrect()).thenReturn(false);
        when(solveLogRepository.findFirstBySolveSessionAndQuestion_QuestionIdOrderBySolveLogIdAsc(
                session, QUESTION_ID))
                .thenReturn(Optional.of(saved));
        when(solveLogRepository.countBySolveSession(session)).thenReturn(2L);

        // 화면에서 답을 바꿔 보냈더라도 처음 저장한 답(C)으로 채점된 결과가 그대로 와야 한다.
        SubmitAnswerResponse response = solveSessionService.submitAnswer(
                SESSION_ID, USER_ID,
                new SubmitAnswerRequest(QUESTION_ID, "A", 10, false, 0, 0, false, false));

        assertThat(response.solveLogId()).isEqualTo(55L);
        assertThat(response.selectedAnswer()).isEqualTo("C");
        assertThat(response.correctAnswer()).isEqualTo("B");
        assertThat(response.isCorrect()).isFalse();
        assertThat(response.explanationText()).isEqualTo("해설");
        assertThat(response.nextQuestionOrder()).isEqualTo(3);
        assertThat(response.isSessionAnswerCompleted()).isFalse();
        verify(solveLogRepository, never()).save(any(SolveLog.class));
    }
}
