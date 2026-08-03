package com.coremate.backend.dailyquiz.service;

import com.coremate.backend.attendance.dto.response.AttendanceRewardResponse;
import com.coremate.backend.attendance.service.AttendanceService;
import com.coremate.backend.content.entity.Concept;
import com.coremate.backend.content.entity.Question;
import com.coremate.backend.content.entity.Subject;
import com.coremate.backend.dailyquiz.dto.request.SubmitDailyQuizAnswerRequest;
import com.coremate.backend.dailyquiz.dto.request.SubmitDailyQuizRequest;
import com.coremate.backend.dailyquiz.dto.response.DailyQuizQuestionResponse;
import com.coremate.backend.dailyquiz.dto.response.DailyQuizResultQuestionResponse;
import com.coremate.backend.dailyquiz.dto.response.DailyQuizResultResponse;
import com.coremate.backend.dailyquiz.dto.response.DailyQuizRewardResponse;
import com.coremate.backend.dailyquiz.dto.response.DailyQuizSetResponse;
import com.coremate.backend.dailyquiz.dto.response.DailyQuizSubmitResponse;
import com.coremate.backend.dailyquiz.entity.DailyQuizAnswerLog;
import com.coremate.backend.dailyquiz.entity.DailyQuizSet;
import com.coremate.backend.dailyquiz.repository.DailyQuizAnswerLogRepository;
import com.coremate.backend.dailyquiz.repository.DailyQuizQuestionPickerRepository;
import com.coremate.backend.dailyquiz.repository.DailyQuizSetRepository;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.problemset.dto.response.QuestionChoiceResponse;
import com.coremate.backend.problemset.dto.type.QuestionFormat;
import com.coremate.backend.problemset.dto.type.QuestionType;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyQuizService {

    private static final int DAILY_QUIZ_QUESTION_COUNT = 6;
    private static final int DAILY_QUIZ_CANDIDATE_POOL_SIZE = 100;
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    private final DailyQuizSetRepository dailyQuizSetRepository;
    private final DailyQuizQuestionPickerRepository dailyQuizQuestionPickerRepository;
    private final DailyQuizAnswerLogRepository dailyQuizAnswerLogRepository;
    private final AttendanceService attendanceService;
    private final ObjectMapper objectMapper;

    // 오늘 날짜의 퀴즈 세트를 조회하고 없으면 승인된 문제 후보에서 생성한다.
    @Transactional
    public DailyQuizSetResponse getTodayDailyQuiz(Long userId) {
        LocalDate today = LocalDate.now();
        List<DailyQuizSet> quizEntries = getOrCreateDailyQuizSet(today);
        boolean solvedToday = attendanceService.hasAttended(userId, today);
        return toSetResponse(quizEntries, solvedToday);
    }

    // 제출된 답안을 오늘 세트와 대조하고, 완료 시 출석/보상 처리를 한 번만 수행한다.
    @Transactional
    public DailyQuizSubmitResponse submitDailyQuiz(Long userId, Long dailyQuizSetId, SubmitDailyQuizRequest request) {
        LocalDate today = LocalDate.now();
        List<DailyQuizSet> quizEntries = getOrCreateDailyQuizSet(today);
        validateDailyQuizSetId(quizEntries, dailyQuizSetId);

        Map<Long, DailyQuizSet> entriesByQuestionId = toEntryMap(quizEntries);
        Map<Long, SubmitDailyQuizAnswerRequest> answersByQuestionId = toAnswerMap(request.answers());
        validateCompletedAnswers(entriesByQuestionId, answersByQuestionId);

        int correctCount = countCorrectAnswers(entriesByQuestionId, answersByQuestionId);
        int totalQuestionCount = quizEntries.size();
        int totalTimeSpentSec = resolveTotalTimeSpentSec(request);
        Long representativeSetId = quizEntries.get(0).getDailyQuizSetId();

        // 문항별 답안 로그를 저장한다(재제출 시 기존 로그를 덮어씀). 해설/이력 조회의 원천이 된다.
        persistAnswerLogs(userId, today, entriesByQuestionId, answersByQuestionId);

        AttendanceRewardResponse attendanceReward = attendanceService.processDailyQuizCompletion(
                userId,
                today,
                null,
                representativeSetId
        );

        return new DailyQuizSubmitResponse(
                null,
                representativeSetId,
                today,
                totalQuestionCount,
                correctCount,
                totalQuestionCount - correctCount,
                totalTimeSpentSec,
                attendanceReward.getAttendanceId() != null,
                toDailyQuizRewardResponse(attendanceReward)
        );
    }

    public DailyQuizRewardResponse getTodayDailyQuizReward(Long userId) {
        return toDailyQuizRewardResponse(attendanceService.getTodayReward(userId));
    }

    // 오늘 내가 제출한 오늘의 퀴즈의 문항별 정답/해설과 내가 선택한 선지를 함께 반환한다.
    // 오늘 제출(풀이)한 기록이 있어야 한다.
    public DailyQuizResultResponse getDailyQuizResult(Long userId) {
        LocalDate today = LocalDate.now();
        List<DailyQuizAnswerLog> logs =
                dailyQuizAnswerLogRepository.findByUserIdAndQuizDateOrderBySortOrderAsc(userId, today);
        if (logs.isEmpty()) {
            // 아직 오늘의 퀴즈를 제출하지 않았으면 정답/해설을 노출하지 않는다.
            throw new CoreMateException(ErrorCode.DAILY_QUIZ_INCOMPLETE);
        }

        List<DailyQuizResultQuestionResponse> questions = logs.stream()
                .map(this::toResultQuestion)
                .toList();

        return new DailyQuizResultResponse(today, questions);
    }

    // 매일 자정(KST) 스케줄러가 호출한다. 오늘 이전 날짜의 선지 기록을 모두 삭제해 DB에 당일 데이터만 남긴다.
    @Transactional
    public long deleteOutdatedAnswerLogs() {
        return dailyQuizAnswerLogRepository.deleteByQuizDateBefore(LocalDate.now(KST_ZONE));
    }

    private void persistAnswerLogs(
            Long userId,
            LocalDate quizDate,
            Map<Long, DailyQuizSet> entriesByQuestionId,
            Map<Long, SubmitDailyQuizAnswerRequest> answersByQuestionId
    ) {
        dailyQuizAnswerLogRepository.deleteByUserIdAndQuizDate(userId, quizDate);

        List<DailyQuizAnswerLog> logs = new ArrayList<>();
        for (Map.Entry<Long, DailyQuizSet> entry : entriesByQuestionId.entrySet()) {
            DailyQuizSet quizEntry = entry.getValue();
            Question question = quizEntry.getQuestion();
            SubmitDailyQuizAnswerRequest answer = answersByQuestionId.get(entry.getKey());
            logs.add(DailyQuizAnswerLog.builder()
                    .userId(userId)
                    .quizDate(quizDate)
                    .question(question)
                    .sortOrder(quizEntry.getSortOrder())
                    .selectedAnswer(answer.selectedAnswer())
                    .build());
        }
        dailyQuizAnswerLogRepository.saveAll(logs);
    }

    private DailyQuizResultQuestionResponse toResultQuestion(DailyQuizAnswerLog log) {
        Question question = log.getQuestion();
        return new DailyQuizResultQuestionResponse(
                question.getQuestionId(),
                log.getSortOrder(),
                resolveSubject(question).getSubjectName(),
                question.getConcept().getConceptName(),
                question.getQuestionText(),
                parseChoices(question.getChoicesJson()),
                question.getCorrectAnswer(),
                question.getExplanationText(),
                log.getSelectedAnswer(),
                isCorrect(log.getSelectedAnswer(), question.getCorrectAnswer())
        );
    }

    private List<DailyQuizSet> getOrCreateDailyQuizSet(LocalDate quizDate) {
        List<DailyQuizSet> existing = dailyQuizSetRepository.findByQuizDateOrderBySortOrderAsc(quizDate);
        if (!existing.isEmpty()) {
            return existing;
        }

        List<Question> selectedQuestions = selectDailyQuizQuestions(quizDate);
        List<DailyQuizSet> newEntries = new ArrayList<>();
        for (int i = 0; i < selectedQuestions.size(); i++) {
            Question question = selectedQuestions.get(i);
            newEntries.add(new DailyQuizSet(
                    quizDate,
                    question,
                    resolveSubject(question),
                    i + 1
            ));
        }
        dailyQuizSetRepository.saveAll(newEntries);
        return dailyQuizSetRepository.findByQuizDateOrderBySortOrderAsc(quizDate);
    }

    private List<Question> selectDailyQuizQuestions(LocalDate quizDate) {
        List<Question> candidates = dailyQuizQuestionPickerRepository.findDailyQuizCandidates(
                PageRequest.of(0, DAILY_QUIZ_CANDIDATE_POOL_SIZE)
        );
        if (candidates.isEmpty()) {
            throw new CoreMateException(ErrorCode.DAILY_QUIZ_NOT_FOUND);
        }

        int questionCount = Math.min(DAILY_QUIZ_QUESTION_COUNT, candidates.size());
        int startIndex = Math.floorMod((int) quizDate.toEpochDay(), candidates.size());
        List<Question> selected = new ArrayList<>();
        for (int i = 0; i < questionCount; i++) {
            selected.add(candidates.get((startIndex + i) % candidates.size()));
        }
        return selected;
    }

    private DailyQuizSetResponse toSetResponse(List<DailyQuizSet> quizEntries, boolean solvedToday) {
        if (quizEntries.isEmpty()) {
            throw new CoreMateException(ErrorCode.DAILY_QUIZ_NOT_FOUND);
        }
        return new DailyQuizSetResponse(
                quizEntries.get(0).getDailyQuizSetId(),
                quizEntries.get(0).getQuizDate(),
                quizEntries.size(),
                solvedToday,
                quizEntries.stream()
                        .map(this::toQuestionResponse)
                        .toList()
        );
    }

    private DailyQuizQuestionResponse toQuestionResponse(DailyQuizSet quizEntry) {
        Question question = quizEntry.getQuestion();
        Concept concept = question.getConcept();
        Subject subject = quizEntry.getSubject();

        return new DailyQuizQuestionResponse(
                question.getQuestionId(),
                subject.getSubjectId(),
                subject.getSubjectName(),
                concept.getConceptId(),
                concept.getConceptName(),
                quizEntry.getSortOrder(),
                parseQuestionFormat(question.getQuestionFormat()),
                parseQuestionType(question.getQuestionType()),
                question.getQuestionText(),
                parseChoices(question.getChoicesJson()),
                (int) question.getDifficultyLevel(),
                question.getExpectedSolveSec(),
                question.getHintCommonMistake() != null
        );
    }

    private Map<Long, DailyQuizSet> toEntryMap(List<DailyQuizSet> quizEntries) {
        Map<Long, DailyQuizSet> entriesByQuestionId = new HashMap<>();
        for (DailyQuizSet quizEntry : quizEntries) {
            entriesByQuestionId.put(quizEntry.getQuestion().getQuestionId(), quizEntry);
        }
        return entriesByQuestionId;
    }

    private Map<Long, SubmitDailyQuizAnswerRequest> toAnswerMap(List<SubmitDailyQuizAnswerRequest> answers) {
        Map<Long, SubmitDailyQuizAnswerRequest> answersByQuestionId = new HashMap<>();
        for (SubmitDailyQuizAnswerRequest answer : answers) {
            if (answersByQuestionId.put(answer.questionId(), answer) != null) {
                throw new CoreMateException(ErrorCode.INVALID_INPUT);
            }
        }
        return answersByQuestionId;
    }

    private void validateCompletedAnswers(
            Map<Long, DailyQuizSet> entriesByQuestionId,
            Map<Long, SubmitDailyQuizAnswerRequest> answersByQuestionId
    ) {
        if (!entriesByQuestionId.keySet().containsAll(answersByQuestionId.keySet())) {
            throw new CoreMateException(ErrorCode.INVALID_INPUT);
        }
        if (!answersByQuestionId.keySet().containsAll(entriesByQuestionId.keySet())) {
            throw new CoreMateException(ErrorCode.DAILY_QUIZ_INCOMPLETE);
        }
    }

    private void validateDailyQuizSetId(List<DailyQuizSet> quizEntries, Long dailyQuizSetId) {
        boolean valid = quizEntries.stream()
                .anyMatch(entry -> entry.getDailyQuizSetId().equals(dailyQuizSetId));
        if (!valid) {
            throw new CoreMateException(ErrorCode.DAILY_QUIZ_NOT_FOUND);
        }
    }

    private int countCorrectAnswers(
            Map<Long, DailyQuizSet> entriesByQuestionId,
            Map<Long, SubmitDailyQuizAnswerRequest> answersByQuestionId
    ) {
        return entriesByQuestionId.entrySet()
                .stream()
                .mapToInt(entry -> isCorrect(entry.getValue().getQuestion(), answersByQuestionId.get(entry.getKey())) ? 1 : 0)
                .sum();
    }

    private boolean isCorrect(Question question, SubmitDailyQuizAnswerRequest answer) {
        return isCorrect(answer.selectedAnswer(), question.getCorrectAnswer());
    }

    private boolean isCorrect(String selectedAnswer, String correctAnswer) {
        String selected = normalizeAnswer(selectedAnswer);
        return !selected.isBlank() && selected.equals(normalizeAnswer(correctAnswer));
    }

    private int resolveTotalTimeSpentSec(SubmitDailyQuizRequest request) {
        if (request.totalTimeSpentSec() != null) {
            return request.totalTimeSpentSec();
        }
        return request.answers()
                .stream()
                .map(SubmitDailyQuizAnswerRequest::timeSpentSec)
                .mapToInt(value -> value == null ? 0 : value)
                .sum();
    }

    private Subject resolveSubject(Question question) {
        return question.getConcept().getCategoryLv2().getCategoryLv1().getSubject();
    }

    private DailyQuizRewardResponse toDailyQuizRewardResponse(AttendanceRewardResponse reward) {
        return new DailyQuizRewardResponse(
                reward.getXpAmount(),
                reward.getCoinAmount(),
                null,
                null,
                reward.getAttendanceId() != null,
                reward.getRewardClaimed()
        );
    }

    private List<QuestionChoiceResponse> parseChoices(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new CoreMateException(ErrorCode.INVALID_CONTENT_DATA);
        }
    }

    private QuestionFormat parseQuestionFormat(String value) {
        try {
            return QuestionFormat.valueOf(value);
        } catch (Exception e) {
            throw new CoreMateException(ErrorCode.INVALID_CONTENT_DATA);
        }
    }

    private QuestionType parseQuestionType(String value) {
        for (QuestionType questionType : QuestionType.values()) {
            if (questionType.name().equalsIgnoreCase(value) || questionType.getValue().equalsIgnoreCase(value)) {
                return questionType;
            }
        }
        throw new CoreMateException(ErrorCode.INVALID_CONTENT_DATA);
    }

    private String normalizeAnswer(String answer) {
        return answer == null ? "" : answer.trim().toUpperCase();
    }
}
