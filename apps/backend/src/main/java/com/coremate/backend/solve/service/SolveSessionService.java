package com.coremate.backend.solve.service;

import com.coremate.backend.content.entity.Concept;
import com.coremate.backend.content.entity.Question;
import com.coremate.backend.content.repository.ConceptRepository;
import com.coremate.backend.content.repository.QuestionRepository;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Transactional
public class SolveSessionService {

    private final ConceptRepository conceptRepository;
    private final QuestionRepository questionRepository;
    private final SolveSessionRepository solveSessionRepository;
    private final SolveLogRepository solveLogRepository;
    private final UserConceptProgressRepository userConceptProgressRepository;
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


    public SubmitAnswerResponse submitAnswer(Long solveSessionId, Long userId, SubmitAnswerRequest request) {
        SolveSession session = solveSessionRepository.findBySolveSessionIdAndUserId(solveSessionId, userId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.SOLVE_SESSION_NOT_FOUND));

        if (session.getStatus() == SolveSessionStatus.COMPLETED) {
            throw new CoreMateException(ErrorCode.SOLVE_SESSION_ALREADY_COMPLETED);
        }

        if (solveLogRepository.existsBySolveSessionAndQuestion_QuestionId(session, request.questionId())) {
            throw new CoreMateException(ErrorCode.ANSWER_ALREADY_SUBMITTED);
        }

        if (session.getConcept() == null) {
            throw new CoreMateException(ErrorCode.QUESTION_NOT_FOUND);
        }

        List<Question> conceptQuestions = questionRepository
                .findAllByConcept_ConceptIdAndActiveTrueAndConceptPracticeEligibleTrueOrderByQuestionIdAsc(
                        session.getConcept().getConceptId());

        Question question = null;
        int order = -1;
        for (int i = 0; i < conceptQuestions.size(); i++) {
            if (conceptQuestions.get(i).getQuestionId().equals(request.questionId())) {
                question = conceptQuestions.get(i);
                order = i + 1;
                break;
            }
        }
        if (question == null) {
            throw new CoreMateException(ErrorCode.QUESTION_NOT_FOUND);
        }

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
        }

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
                0,
                0
        );
    }

    @Transactional(readOnly = true)
    public ResumeSessionResponse resumeSession(Long userId, Long conceptId) {
        SolveSession session = solveSessionRepository
                .findByUserIdAndConcept_ConceptIdAndStatus(userId, conceptId, SolveSessionStatus.IN_PROGRESS)
                .orElseThrow(() -> new CoreMateException(ErrorCode.SOLVE_SESSION_NOT_FOUND));

        List<Question> questions =
                questionRepository.findAllByConcept_ConceptIdAndActiveTrueAndConceptPracticeEligibleTrueOrderByQuestionIdAsc(conceptId);

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
                questions.stream().map(Question::getQuestionId).toList(),
                answeredLogs
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
