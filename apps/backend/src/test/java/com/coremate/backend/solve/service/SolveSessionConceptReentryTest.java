package com.coremate.backend.solve.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coremate.backend.content.entity.Concept;
import com.coremate.backend.global.idempotency.IdempotencyGuard;
import com.coremate.backend.content.entity.Question;
import com.coremate.backend.content.repository.CategoryLv1Repository;
import com.coremate.backend.content.repository.CategoryLv2Repository;
import com.coremate.backend.content.repository.ConceptRepository;
import com.coremate.backend.content.repository.QuestionRepository;
import com.coremate.backend.content.repository.SubjectRepository;
import com.coremate.backend.learninghistory.service.LearningHistoryService;
import com.coremate.backend.progress.repository.UserConceptProgressRepository;
import com.coremate.backend.solve.dto.request.CreateSolveSessionRequest;
import com.coremate.backend.solve.dto.response.StartSolveSessionResponse;
import com.coremate.backend.solve.dto.type.SolveSessionStatus;
import com.coremate.backend.solve.dto.type.SolveSessionType;
import com.coremate.backend.solve.entity.SolveSession;
import com.coremate.backend.solve.repository.SolveLogRepository;
import com.coremate.backend.solve.repository.SolveSessionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

/**
 * 개념 문제집 재진입 시 기존 진행 중 세션을 중단(soft-abandon)하고 새 세션을 발급하는지 검증한다.
 *
 * <p>개념 문제집은 나가기(exit) 시 상태를 바꾸는 경로가 없어 세션이 IN_PROGRESS 로 남는다.
 * 예전에는 이때 재진입하면 예외를 던져 영구히 막혔으므로, 기존 세션을 중단 처리하고
 * 처음부터 새로 풀도록 바뀌었는지 확인한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("개념 문제집 재진입")
class SolveSessionConceptReentryTest {

    private static final Long USER_ID = 7L;
    private static final Long CONCEPT_ID = 42L;

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

        Concept concept = org.mockito.Mockito.mock(Concept.class);
        when(concept.getConceptName()).thenReturn("스택");
        when(conceptRepository.findByConceptIdAndActiveTrue(CONCEPT_ID)).thenReturn(Optional.of(concept));

        Question question = org.mockito.Mockito.mock(Question.class);
        when(question.getQuestionId()).thenReturn(1001L);
        when(questionRepository
                .findAllByConcept_ConceptIdAndActiveTrueAndConceptPracticeEligibleTrueOrderByQuestionIdAsc(CONCEPT_ID))
                .thenReturn(List.of(question));
    }

    @Test
    @DisplayName("진행 중 세션이 있으면 중단 처리하고 새 세션을 발급한다")
    void 진행중_세션을_중단하고_새로_시작한다() {
        SolveSession inProgress = org.mockito.Mockito.mock(SolveSession.class);
        when(solveSessionRepository.findByUserIdAndConcept_ConceptIdAndStatus(
                USER_ID, CONCEPT_ID, SolveSessionStatus.IN_PROGRESS))
                .thenReturn(Optional.of(inProgress));

        StartSolveSessionResponse response = solveSessionService.startSolveSession(
                USER_ID, new CreateSolveSessionRequest(SolveSessionType.CONCEPT_PRACTICE, CONCEPT_ID));

        verify(inProgress).abandon();
        verify(solveSessionRepository).save(any(SolveSession.class));
        assertThat(response.status()).isEqualTo(SolveSessionStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("진행 중 세션이 없으면 중단 없이 새 세션을 발급한다")
    void 진행중_세션이_없으면_그냥_시작한다() {
        when(solveSessionRepository.findByUserIdAndConcept_ConceptIdAndStatus(
                USER_ID, CONCEPT_ID, SolveSessionStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());

        solveSessionService.startSolveSession(
                USER_ID, new CreateSolveSessionRequest(SolveSessionType.CONCEPT_PRACTICE, CONCEPT_ID));

        verify(solveSessionRepository).save(any(SolveSession.class));
        verify(solveSessionRepository, never()).delete(any(SolveSession.class));
    }
}
