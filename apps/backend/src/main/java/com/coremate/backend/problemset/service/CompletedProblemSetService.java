package com.coremate.backend.problemset.service;

import com.coremate.backend.content.entity.CategoryLv1;
import com.coremate.backend.content.entity.CategoryLv2;
import com.coremate.backend.content.entity.Concept;
import com.coremate.backend.content.entity.Question;
import com.coremate.backend.content.entity.Subject;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.problemset.dto.response.CompletedProblemSetDetailResponse;
import com.coremate.backend.problemset.dto.response.CompletedProblemSetLogResponse;
import com.coremate.backend.problemset.dto.response.CompletedProblemSetSummaryResponse;
import com.coremate.backend.problemset.dto.response.QuestionChoiceResponse;
import com.coremate.backend.solve.dto.type.SolveSessionStatus;
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
@Transactional(readOnly = true)
public class CompletedProblemSetService {

    private final SolveSessionRepository solveSessionRepository;
    private final SolveLogRepository solveLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * 사용자의 완료된 문제집 회차 목록을 필터별로 조회한다.
     * 필터가 null 이면 조건에서 제외된다.
     */
    public List<CompletedProblemSetSummaryResponse> getCompletedProblemSets(Long userId,
            Long subjectId, Long categoryLv1Id, Long categoryLv2Id) {
        List<SolveSession> sessions = solveSessionRepository.findCompletedSessions(
                userId, SolveSessionStatus.COMPLETED, subjectId, categoryLv1Id, categoryLv2Id);

        return sessions.stream().map(this::toSummary).toList();
    }

    /**
     * 완료된 문제집 회차 상세 (문제별 풀이 결과 포함)를 조회한다.
     * 본인의 완료된 회차만 접근 가능하다.
     */
    public CompletedProblemSetDetailResponse getCompletedProblemSetDetail(Long userId, Long solveSessionId) {
        SolveSession session = solveSessionRepository.findBySolveSessionIdAndUserId(solveSessionId, userId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.SOLVE_SESSION_NOT_FOUND));

        if (session.getStatus() != SolveSessionStatus.COMPLETED) {
            throw new CoreMateException(ErrorCode.SOLVE_SESSION_NOT_FOUND);
        }

        List<SolveLog> logs = solveLogRepository.findAllBySolveSessionOrderByQuestionOrder(session);
        List<CompletedProblemSetLogResponse> logResponses = logs.stream()
                .map(this::toLogResponse)
                .toList();

        return new CompletedProblemSetDetailResponse(
                session.getSolveSessionId(),
                session.getSessionType(),
                session.getTitle(),
                subjectId(session), subjectName(session),
                categoryLv1Id(session), categoryLv1Name(session),
                categoryLv2Id(session), categoryLv2Name(session),
                conceptId(session), conceptName(session),
                session.getTotalQuestionCount(),
                session.getCorrectCount(),
                session.getWrongCount(),
                session.getCorrectRate(),
                session.getTotalTimeSpentSec(),
                session.getCompletedAt(),
                logResponses
        );
    }

    // ---------- private helpers ----------

    private CompletedProblemSetSummaryResponse toSummary(SolveSession session) {
        return new CompletedProblemSetSummaryResponse(
                session.getSolveSessionId(),
                session.getSessionType(),
                session.getTitle(),
                subjectId(session), subjectName(session),
                categoryLv1Id(session), categoryLv1Name(session),
                categoryLv2Id(session), categoryLv2Name(session),
                conceptId(session), conceptName(session),
                session.getTotalQuestionCount(),
                session.getCorrectCount(),
                session.getWrongCount(),
                session.getCorrectRate(),
                session.getTotalTimeSpentSec(),
                session.getCompletedAt()
        );
    }

    private CompletedProblemSetLogResponse toLogResponse(SolveLog log) {
        Question question = log.getQuestion();
        return new CompletedProblemSetLogResponse(
                log.getSolveLogId(),
                question.getQuestionId(),
                log.getQuestionOrder(),
                question.getQuestionText(),
                parseChoices(question.getChoicesJson()),
                log.getSelectedAnswer(),
                log.getCorrectAnswerSnapshot(),
                log.isCorrect(),
                question.getExplanationText(),
                log.getTimeSpentSec(),
                log.getExpectedSolveSecSnapshot(),
                log.isHintUsed(),
                log.getHintCount(),
                log.getOptionChangeCount(),
                log.isAnswerChanged(),
                log.isBookmarked()
        );
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

    private Long subjectId(SolveSession s) {
        Subject subject = s.getSubject();
        return subject == null ? null : subject.getSubjectId();
    }

    private String subjectName(SolveSession s) {
        Subject subject = s.getSubject();
        return subject == null ? null : subject.getSubjectName();
    }

    private Long categoryLv1Id(SolveSession s) {
        CategoryLv1 lv1 = s.getCategoryLv1();
        return lv1 == null ? null : lv1.getCategoryLv1Id();
    }

    private String categoryLv1Name(SolveSession s) {
        CategoryLv1 lv1 = s.getCategoryLv1();
        return lv1 == null ? null : lv1.getCategoryLv1Name();
    }

    private Long categoryLv2Id(SolveSession s) {
        CategoryLv2 lv2 = s.getCategoryLv2();
        return lv2 == null ? null : lv2.getCategoryLv2Id();
    }

    private String categoryLv2Name(SolveSession s) {
        CategoryLv2 lv2 = s.getCategoryLv2();
        return lv2 == null ? null : lv2.getCategoryLv2Name();
    }

    private Long conceptId(SolveSession s) {
        Concept concept = s.getConcept();
        return concept == null ? null : concept.getConceptId();
    }

    private String conceptName(SolveSession s) {
        Concept concept = s.getConcept();
        return concept == null ? null : concept.getConceptName();
    }
}
