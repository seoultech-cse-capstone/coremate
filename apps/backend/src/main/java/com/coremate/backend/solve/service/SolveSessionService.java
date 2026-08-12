package com.coremate.backend.solve.service;

import com.coremate.backend.content.entity.CategoryLv1;
import com.coremate.backend.content.entity.CategoryLv2;
import com.coremate.backend.content.entity.Concept;
import com.coremate.backend.content.entity.Question;
import com.coremate.backend.content.entity.Subject;
import com.coremate.backend.content.repository.CategoryLv1Repository;
import com.coremate.backend.content.repository.CategoryLv2Repository;
import com.coremate.backend.content.repository.ConceptRepository;
import com.coremate.backend.content.repository.QuestionRepository;
import com.coremate.backend.content.repository.SubjectRepository;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.learninghistory.service.LearningHistoryService;
import com.coremate.backend.problemset.dto.response.QuestionChoiceResponse;
import com.coremate.backend.progress.entity.UserConceptProgress;
import com.coremate.backend.progress.repository.UserConceptProgressRepository;
import com.coremate.backend.solve.dto.request.CompleteSolveSessionRequest;
import com.coremate.backend.solve.dto.request.CreateSolveSessionRequest;
import com.coremate.backend.solve.dto.request.GuestAnswerRequest;
import com.coremate.backend.solve.dto.request.SubmitAnswerRequest;
import com.coremate.backend.solve.dto.response.CompleteSolveSessionResponse;
import com.coremate.backend.solve.dto.response.GuestAnswerResultResponse;
import com.coremate.backend.solve.dto.response.QuestionSolveResponse;
import com.coremate.backend.solve.dto.response.ResumeSessionResponse;
import com.coremate.backend.solve.dto.response.StartSolveSessionResponse;
import com.coremate.backend.solve.dto.response.SubmitAnswerResponse;
import com.coremate.backend.solve.dto.type.SolveSessionStatus;
import com.coremate.backend.solve.dto.type.SolveSessionType;
import com.coremate.backend.solve.entity.SolveLog;
import com.coremate.backend.solve.entity.SolveSession;
import com.coremate.backend.solve.repository.SolveLogRepository;
import com.coremate.backend.solve.repository.SolveSessionRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SolveSessionService {

    // 모의고사 문항 수 (2026-07-30 팀 확정)
    // 출제 가능한 문항이 부족하면 selectMockQuestions가 있는 만큼만 담는다.
    private static final int CHAPTER_MOCK_TOTAL = 20;
    private static final int SUBJECT_MOCK_TOTAL = 30;

    // 모의고사 회차 제한 시간 (2026-08-03 팀 확정)
    // 회차에 저장하지 않고 유형별 고정값으로 관리한다. 개념 문제집과 오늘의 CS 퀴즈는 제한이 없다.
    private static final Duration CHAPTER_MOCK_TIME_LIMIT = Duration.ofMinutes(40);
    private static final Duration SUBJECT_MOCK_TIME_LIMIT = Duration.ofMinutes(60);

    private static final Set<SolveSessionType> MOCK_TYPES = Set.of(
            SolveSessionType.CATEGORY_LV1_MOCK,
            SolveSessionType.CATEGORY_LV2_MOCK,
            SolveSessionType.SUBJECT_MOCK);

    private final SubjectRepository subjectRepository;
    private final CategoryLv1Repository categoryLv1Repository;
    private final CategoryLv2Repository categoryLv2Repository;
    private final ConceptRepository conceptRepository;
    private final QuestionRepository questionRepository;
    private final SolveSessionRepository solveSessionRepository;
    private final SolveLogRepository solveLogRepository;
    private final UserConceptProgressRepository userConceptProgressRepository;
    private final LearningHistoryService learningHistoryService;
    private final SolveRewardService solveRewardService;
    private final SolveSessionExpiryService solveSessionExpiryService;
    private final ObjectMapper objectMapper;

    public StartSolveSessionResponse startSolveSession(Long userId, CreateSolveSessionRequest request) {
        if (request.sessionType() != SolveSessionType.CONCEPT_PRACTICE) {
            throw new CoreMateException(ErrorCode.UNSUPPORTED_SESSION_TYPE);
        }

        Long conceptId = request.targetId();

        Concept concept = conceptRepository.findByConceptIdAndActiveTrue(conceptId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.CONCEPT_NOT_FOUND));

        List<Question> questions =
                questionRepository.findAllByConcept_ConceptIdAndActiveTrueAndConceptPracticeEligibleTrueOrderByQuestionIdAsc(conceptId);

        if (questions.isEmpty()) {
            throw new CoreMateException(ErrorCode.QUESTION_NOT_FOUND);
        }

        List<QuestionSolveResponse> questionResponses = questions.stream()
                .map(q -> new QuestionSolveResponse(
                        q.getQuestionId(),
                        q.getQuestionText(),
                        parseChoices(q.getChoicesJson())
                ))
                .toList();

        String title = concept.getConceptName() + " 개념 문제집";

        if (userId == null) {
            return new StartSolveSessionResponse(
                    null, title, SolveSessionType.CONCEPT_PRACTICE,
                    questions.size(), null, null, questionResponses
            );
        }

        // 같은 개념으로 재진입하면 기존 진행중 세션을 중단 처리하고 처음부터 새로 시작한다.
        // 개념 문제집은 나가기(exit) 시 상태를 바꾸는 경로가 없어 세션이 IN_PROGRESS로 남는데,
        // 예외를 던지면 재진입이 영구히 막히므로 이전 세션과 로그는 무시하고 새 세션을 발급한다.
        abandonInProgressConceptSession(userId, conceptId);

        SolveSession session = SolveSession.builder()
                .userId(userId)
                .sessionType(SolveSessionType.CONCEPT_PRACTICE)
                .concept(concept)
                .title(title)
                .totalQuestionCount(questions.size())
                .questionIdsJson(writeQuestionIds(questions))
                .build();

        solveSessionRepository.save(session);

        return new StartSolveSessionResponse(
                session.getSolveSessionId(), title, SolveSessionType.CONCEPT_PRACTICE,
                questions.size(), session.getStatus(), session.getStartedAt(), questionResponses
        );
    }

    @Transactional(readOnly = true)
    public List<GuestAnswerResultResponse> checkGuestAnswers(GuestAnswerRequest request) {
        Long conceptId = request.conceptId();
        return request.answers().stream()
                .map(item -> {
                    Question question = questionRepository
                            .findByQuestionIdAndConcept_ConceptIdAndActiveTrueAndConceptPracticeEligibleTrue(
                                    item.questionId(), conceptId)
                            .orElseThrow(() -> new CoreMateException(ErrorCode.QUESTION_NOT_FOUND));
                    boolean isCorrect = question.getCorrectAnswer().equals(item.selectedAnswer());
                    return new GuestAnswerResultResponse(
                            question.getQuestionId(),
                            item.selectedAnswer(),
                            question.getCorrectAnswer(),
                            isCorrect,
                            question.getExplanationText()
                    );
                })
                .toList();
    }

    private List<QuestionChoiceResponse> parseChoices(String choicesJson) {
        if (choicesJson == null || choicesJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(choicesJson, new TypeReference<>() {});
        } catch (Exception e) {
            throw new CoreMateException(ErrorCode.INVALID_CONTENT_DATA);
        }
    }

    /**
     * 세션 시작 시 결정된 questionId 목록을 JSON 문자열로 직렬화한다.
     */
    private String writeQuestionIds(List<Question> questions) {
        List<Long> ids = questions.stream().map(Question::getQuestionId).toList();
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (Exception e) {
            throw new CoreMateException(ErrorCode.INVALID_CONTENT_DATA);
        }
    }

    /**
     * SolveSession.questionIdsJson을 List로 파싱한다.
     */
    private List<Long> parseQuestionIds(String questionIdsJson) {
        if (questionIdsJson == null || questionIdsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(questionIdsJson, new TypeReference<>() {});
        } catch (Exception e) {
            throw new CoreMateException(ErrorCode.INVALID_CONTENT_DATA);
        }
    }


    public SubmitAnswerResponse submitAnswer(Long solveSessionId, Long userId, SubmitAnswerRequest request) {
        SolveSession session = solveSessionRepository.findBySolveSessionIdAndUserId(solveSessionId, userId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.SOLVE_SESSION_NOT_FOUND));

        requireInProgress(session);
        rejectIfExpired(session);

        if (solveLogRepository.existsBySolveSessionAndQuestion_QuestionId(session, request.questionId())) {
            throw new CoreMateException(ErrorCode.ANSWER_ALREADY_SUBMITTED);
        }

        // 세션 시작 시 결정된 questionId 목록에서 순서(order)를 결정론적으로 계산한다.
        // 개념 문제집 / 모의고사 모두 동일한 방식으로 처리 (concept 유무와 무관).
        List<Long> sessionQuestionIds = parseQuestionIds(session.getQuestionIdsJson());
        int order = sessionQuestionIds.indexOf(request.questionId()) + 1;
        if (order <= 0) {
            throw new CoreMateException(ErrorCode.QUESTION_NOT_FOUND);
        }

        Question question = questionRepository.findById(request.questionId())
                .orElseThrow(() -> new CoreMateException(ErrorCode.QUESTION_NOT_FOUND));

        boolean isCorrect = question.getCorrectAnswer().equals(request.selectedAnswer());

        SolveLog log = SolveLog.builder()
                .solveSession(session)
                .userId(userId)
                .question(question)
                .questionOrder(order)
                .selectedAnswer(request.selectedAnswer())
                .correctAnswerSnapshot(question.getCorrectAnswer())
                .correct(isCorrect)
                .timeSpentSec(request.timeSpentSec())
                .expectedSolveSecSnapshot(question.getExpectedSolveSec())
                .hintUsed(request.hintUsed())
                .hintCount(request.hintCount())
                .optionChangeCount(request.optionChangeCount())
                .answerChanged(request.isAnswerChanged())
                .bookmarked(request.isBookmarked())
                .build();

        solveLogRepository.save(log);

        long totalSubmitted = solveLogRepository.countBySolveSession(session);
        boolean isSessionAnswerCompleted = totalSubmitted >= session.getTotalQuestionCount();

        Integer nextQuestionOrder = isSessionAnswerCompleted || order >= session.getTotalQuestionCount()
                ? null
                : order + 1;

        return new SubmitAnswerResponse(
                log.getSolveLogId(),
                question.getQuestionId(),
                request.selectedAnswer(),
                question.getCorrectAnswer(),
                isCorrect,
                question.getExplanationText(),
                nextQuestionOrder,
                isSessionAnswerCompleted
        );
    }

    public CompleteSolveSessionResponse completeSolveSession(Long solveSessionId, Long userId,
            CompleteSolveSessionRequest request) {
        SolveSession session = solveSessionRepository.findBySolveSessionIdAndUserId(solveSessionId, userId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.SOLVE_SESSION_NOT_FOUND));

        requireInProgress(session);
        rejectIfExpired(session);

        List<SolveLog> logs = solveLogRepository.findAllBySolveSessionOrderByQuestionOrder(session);

        if (logs.size() < session.getTotalQuestionCount()) {
            throw new CoreMateException(ErrorCode.SOLVE_NOT_ALL_ANSWERED);
        }

        int correctCount = (int) logs.stream().filter(SolveLog::isCorrect).count();
        int wrongCount = logs.size() - correctCount;
        int totalTimeSpentSec = request.totalTimeSpentSec() == null ? 0 : request.totalTimeSpentSec();

        session.complete(correctCount, wrongCount, totalTimeSpentSec);

        if (session.getSessionType() == SolveSessionType.CONCEPT_PRACTICE && session.getConcept() != null) {
            updateConceptProgress(userId, session.getConcept().getConceptId(), session.getSolveSessionId());
        } else if (session.getSessionType().isMock()) {
            // 모의고사는 이미 완료된 개념만 progress를 최신 회차로 갱신한다.
            // 미완료 개념은 모의고사 참여만으로 완료 처리하지 않고 그대로 둔다.
            List<Long> conceptIdsInSession = logs.stream()
                    .map(log -> log.getQuestion().getConcept().getConceptId())
                    .distinct()
                    .toList();
            Set<Long> alreadyCompletedConceptIds = userConceptProgressRepository
                    .findCompletedConceptIds(userId, conceptIdsInSession);
            for (Long cid : alreadyCompletedConceptIds) {
                updateConceptProgress(userId, cid, session.getSolveSessionId());
            }
        }

        // Learning History 누적 (완료 시점, 세션당 1회)
        learningHistoryService.accumulate(session, logs, correctCount);

        BigDecimal correctRate = calculateCorrectRate(correctCount, logs.size());
        SolveRewardResult reward = grantRewardSafely(session, correctRate);

        return new CompleteSolveSessionResponse(
                session.getSolveSessionId(),
                session.getTitle(),
                session.getSessionType(),
                session.getTotalQuestionCount(),
                correctCount,
                wrongCount,
                session.getTotalTimeSpentSec(),
                session.getStatus(),
                session.getCompletedAt(),
                reward.earnedXp(),
                reward.earnedCoin(),
                correctRate,
                reward.xpSkipReason(),
                reward.coinSkipReason()
        );
    }

    // 보상 지급이 실패해도 풀이 완료는 그대로 확정한다.
    // 보상은 학습 기록보다 부수적이라, 보상 로직 문제로 사용자가 푼 기록이 사라지면 안 된다.
    // SolveRewardService가 REQUIRES_NEW로 별도 트랜잭션을 쓰므로 여기서 잡아도
    // 완료 트랜잭션이 rollback-only로 오염되지 않는다.
    private SolveRewardResult grantRewardSafely(SolveSession session, BigDecimal correctRate) {
        try {
            return solveRewardService.grantForCompletedSession(session, correctRate);
        } catch (Exception e) {
            log.error("보상 지급 실패. 풀이 완료는 유지한다. solveSessionId={}, userId={}",
                    session.getSolveSessionId(), session.getUserId(), e);
            return SolveRewardResult.none();
        }
    }

    // 정답률(%)을 소수점 2자리로 계산한다. 코인 지급 기준(min_correct_rate)과 비교된다.
    private BigDecimal calculateCorrectRate(int correctCount, int totalCount) {
        if (totalCount <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(correctCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public ResumeSessionResponse resumeSession(Long userId, Long conceptId) {
        SolveSession session = solveSessionRepository
                .findByUserIdAndConcept_ConceptIdAndStatus(userId, conceptId, SolveSessionStatus.IN_PROGRESS)
                .orElseThrow(() -> new CoreMateException(ErrorCode.SOLVE_SESSION_NOT_FOUND));

        List<Long> sessionQuestionIds = parseQuestionIds(session.getQuestionIdsJson());

        List<SolveLog> logs = solveLogRepository.findAllBySolveSessionOrderByQuestionOrder(session);

        List<ResumeSessionResponse.AnsweredLog> answeredLogs = logs.stream()
                .map(log -> new ResumeSessionResponse.AnsweredLog(
                        log.getQuestion().getQuestionId(),
                        log.getQuestionOrder(),
                        log.getSelectedAnswer()
                ))
                .toList();

        return new ResumeSessionResponse(
                session.getSolveSessionId(),
                session.getTitle(),
                session.getSessionType(),
                session.getTotalQuestionCount(),
                session.getStartedAt(),
                sessionQuestionIds,
                answeredLogs
        );
    }

    public StartSolveSessionResponse startCategoryLv1MockSession(Long userId, Long categoryLv1Id) {
        CategoryLv1 categoryLv1 = categoryLv1Repository.findByCategoryLv1IdAndActiveTrue(categoryLv1Id)
                .orElseThrow(() -> new CoreMateException(ErrorCode.CATEGORY_LV1_NOT_FOUND));

        List<Concept> concepts = conceptRepository
                .findAllByCategoryLv2_CategoryLv1_CategoryLv1IdAndActiveTrue(categoryLv1Id);

        List<Long> conceptIds = concepts.stream().map(Concept::getConceptId).toList();
        List<Question> questions = selectMockQuestions(conceptIds, CHAPTER_MOCK_TOTAL);

        String title = categoryLv1.getCategoryLv1Name() + " 챕터 모의고사";

        abandonInProgressMockSessions(userId);

        SolveSession session = SolveSession.builder()
                .userId(userId)
                .sessionType(SolveSessionType.CATEGORY_LV1_MOCK)
                .subject(categoryLv1.getSubject())
                .categoryLv1(categoryLv1)
                .title(title)
                .totalQuestionCount(questions.size())
                .questionIdsJson(writeQuestionIds(questions))
                .build();
        solveSessionRepository.save(session);

        return buildStartResponse(session, title, SolveSessionType.CATEGORY_LV1_MOCK, questions);
    }

    public StartSolveSessionResponse startCategoryLv2MockSession(Long userId, Long categoryLv2Id) {
        CategoryLv2 categoryLv2 = categoryLv2Repository.findByCategoryLv2IdAndActiveTrue(categoryLv2Id)
                .orElseThrow(() -> new CoreMateException(ErrorCode.CATEGORY_LV2_NOT_FOUND));

        List<Concept> concepts = conceptRepository
                .findAllByCategoryLv2_CategoryLv2IdAndActiveTrueOrderBySortOrderAsc(categoryLv2Id);

        List<Long> conceptIds = concepts.stream().map(Concept::getConceptId).toList();
        List<Question> questions = selectMockQuestions(conceptIds, CHAPTER_MOCK_TOTAL);

        String title = categoryLv2.getCategoryLv2Name() + " 챕터 모의고사";

        abandonInProgressMockSessions(userId);

        SolveSession session = SolveSession.builder()
                .userId(userId)
                .sessionType(SolveSessionType.CATEGORY_LV2_MOCK)
                .subject(categoryLv2.getCategoryLv1().getSubject())
                .categoryLv1(categoryLv2.getCategoryLv1())
                .categoryLv2(categoryLv2)
                .title(title)
                .totalQuestionCount(questions.size())
                .questionIdsJson(writeQuestionIds(questions))
                .build();
        solveSessionRepository.save(session);

        return buildStartResponse(session, title, SolveSessionType.CATEGORY_LV2_MOCK, questions);
    }

    public StartSolveSessionResponse startSubjectMockSession(Long userId, Long subjectId) {
        Subject subject = subjectRepository.findBySubjectIdAndActiveTrue(subjectId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.SUBJECT_NOT_FOUND));

        List<Concept> concepts = conceptRepository
                .findAllByCategoryLv2_CategoryLv1_Subject_SubjectIdAndActiveTrue(subjectId);

        List<Long> conceptIds = concepts.stream().map(Concept::getConceptId).toList();
        List<Question> questions = selectMockQuestions(conceptIds, SUBJECT_MOCK_TOTAL);

        String title = subject.getSubjectName() + " 과목 모의고사";

        abandonInProgressMockSessions(userId);

        SolveSession session = SolveSession.builder()
                .userId(userId)
                .sessionType(SolveSessionType.SUBJECT_MOCK)
                .subject(subject)
                .title(title)
                .totalQuestionCount(questions.size())
                .questionIdsJson(writeQuestionIds(questions))
                .build();
        solveSessionRepository.save(session);

        return buildStartResponse(session, title, SolveSessionType.SUBJECT_MOCK, questions);
    }

    /**
     * 진행 중인 회차에만 답안 제출과 완료 처리를 허용한다.
     * 중단된 회차는 클라이언트가 옛 회차 ID를 들고 있어도 더 이상 받지 않는다.
     */
    private void requireInProgress(SolveSession session) {
        if (session.getStatus() == SolveSessionStatus.COMPLETED) {
            throw new CoreMateException(ErrorCode.SOLVE_SESSION_ALREADY_COMPLETED);
        }
        if (session.getStatus() == SolveSessionStatus.ABANDONED) {
            throw new CoreMateException(ErrorCode.SOLVE_SESSION_ABANDONED);
        }
    }

    /**
     * 새 모의고사를 시작하기 전에 사용자의 진행 중 모의고사 회차를 중단 처리한다.
     * 회차와 풀이 로그는 지우지 않고 상태만 바꾼다. 개념 문제집 회차는 대상이 아니다.
     */
    private void abandonInProgressMockSessions(Long userId) {
        solveSessionRepository
                .findByUserIdAndStatusAndSessionTypeIn(userId, SolveSessionStatus.IN_PROGRESS, MOCK_TYPES)
                .forEach(SolveSession::abandon);
    }

    /**
     * 같은 개념으로 개념 문제집을 다시 시작하기 전에 기존 진행 중 세션을 중단 처리한다.
     * 세션과 풀이 로그는 지우지 않고 상태만 바꾸며, 새 세션이 처음부터 풀리도록 한다.
     */
    private void abandonInProgressConceptSession(Long userId, Long conceptId) {
        solveSessionRepository
                .findByUserIdAndConcept_ConceptIdAndStatus(userId, conceptId, SolveSessionStatus.IN_PROGRESS)
                .ifPresent(SolveSession::abandon);
    }

    /**
     * 제한 시간이 지난 모의고사 회차는 제출/완료를 거부하고 중단 처리한다.
     * 별도 배치 없이 회차에 접근하는 시점에 판정한다.
     *
     * <p>거부는 예외로 처리되어 이 트랜잭션이 롤백되므로, 중단 처리는 별도 트랜잭션에 맡긴다.
     */
    private void rejectIfExpired(SolveSession session) {
        if (!session.getSessionType().isMock()) {
            return;
        }
        Duration limit = session.getSessionType() == SolveSessionType.SUBJECT_MOCK
                ? SUBJECT_MOCK_TIME_LIMIT
                : CHAPTER_MOCK_TIME_LIMIT;
        if (LocalDateTime.now().isBefore(session.getStartedAt().plus(limit))) {
            return;
        }
        solveSessionExpiryService.abandon(session.getSolveSessionId());
        throw new CoreMateException(ErrorCode.SOLVE_SESSION_EXPIRED);
    }

    /**
     * 모의고사 문제 선정.
     * 완료/미완료 개념 구분 없이, 지정한 conceptIds 범위 내에서 total 문제 수만큼 랜덤하게 선정한다.
     * 이미 푼 문제 여부와 무관하게 전부 랜덤이다.
     */
    private List<Question> selectMockQuestions(List<Long> conceptIds, int total) {
        if (conceptIds.isEmpty()) {
            throw new CoreMateException(ErrorCode.QUESTION_NOT_FOUND);
        }

        List<Question> selected = questionRepository.findRandomMockQuestionsByConceptIds(conceptIds, total);

        if (selected.isEmpty()) {
            throw new CoreMateException(ErrorCode.QUESTION_NOT_FOUND);
        }
        return selected;
    }

    private StartSolveSessionResponse buildStartResponse(SolveSession session, String title,
            SolveSessionType sessionType, List<Question> questions) {
        List<QuestionSolveResponse> questionResponses = questions.stream()
                .map(q -> new QuestionSolveResponse(
                        q.getQuestionId(),
                        q.getQuestionText(),
                        parseChoices(q.getChoicesJson())
                ))
                .toList();
        return new StartSolveSessionResponse(
                session.getSolveSessionId(),
                title,
                sessionType,
                questions.size(),
                session.getStatus(),
                session.getStartedAt(),
                questionResponses
        );
    }

    private void updateConceptProgress(Long userId, Long conceptId, Long solveSessionId) {
        UserConceptProgress progress = userConceptProgressRepository
                .findByUserIdAndConceptId(userId, conceptId)
                .orElseGet(() -> userConceptProgressRepository.save(
                        UserConceptProgress.builder()
                                .userId(userId)
                                .conceptId(conceptId)
                                .build()
                ));

        progress.markCompleted(solveSessionId);
    }
}
