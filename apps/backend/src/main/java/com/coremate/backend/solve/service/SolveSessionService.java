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
import java.util.Collections;
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Transactional
public class SolveSessionService {

    // 문제 수 정책 (추후 조정 가능)
    private static final int CHAPTER_MOCK_TOTAL = 20;
    private static final int SUBJECT_MOCK_TOTAL = 50;

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

        if (solveSessionRepository.existsByUserIdAndConcept_ConceptIdAndStatus(
                userId, conceptId, SolveSessionStatus.IN_PROGRESS)) {
            throw new CoreMateException(ErrorCode.SOLVE_SESSION_ALREADY_IN_PROGRESS);
        }

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

        if (session.getStatus() == SolveSessionStatus.COMPLETED) {
            throw new CoreMateException(ErrorCode.SOLVE_SESSION_ALREADY_COMPLETED);
        }

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

        if (session.getStatus() == SolveSessionStatus.COMPLETED) {
            throw new CoreMateException(ErrorCode.SOLVE_SESSION_ALREADY_COMPLETED);
        }

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
        SolveRewardResult reward = solveRewardService.grantForCompletedSession(session, correctRate);

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
