package com.coremate.backend.dailyquiz.service;

import com.coremate.backend.attendance.dto.response.AttendanceRewardResponse;
import com.coremate.backend.attendance.service.AttendanceService;
import com.coremate.backend.content.entity.Concept;
import com.coremate.backend.content.entity.Question;
import com.coremate.backend.content.entity.Subject;
import com.coremate.backend.dailyquiz.dto.request.SubmitDailyQuizAnswerRequest;
import com.coremate.backend.dailyquiz.dto.request.SubmitDailyQuizRequest;
import com.coremate.backend.dailyquiz.dto.response.DailyQuizQuestionResponse;
import com.coremate.backend.dailyquiz.dto.response.DailyQuizRewardResponse;
import com.coremate.backend.dailyquiz.dto.response.DailyQuizSetResponse;
import com.coremate.backend.dailyquiz.dto.response.DailyQuizSubmitResponse;
import com.coremate.backend.dailyquiz.entity.DailyQuizSet;
import com.coremate.backend.dailyquiz.repository.DailyQuizQuestionPickerRepository;
import com.coremate.backend.dailyquiz.repository.DailyQuizSetRepository;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.problemset.dto.response.QuestionChoiceResponse;
import com.coremate.backend.problemset.dto.type.QuestionFormat;
import com.coremate.backend.problemset.dto.type.QuestionType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyQuizService {

    private static final int DAILY_QUIZ_QUESTION_COUNT = 6;

    private final DailyQuizSetRepository dailyQuizSetRepository;
    private final DailyQuizQuestionPickerRepository dailyQuizQuestionPickerRepository;
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
        List<Long> candidateIds = dailyQuizQuestionPickerRepository.findDailyQuizCandidateIds();
        if (candidateIds.isEmpty()) {
            throw new CoreMateException(ErrorCode.DAILY_QUIZ_NOT_FOUND);
        }

        List<Long> selectedIds = selectDailyQuizQuestionIds(candidateIds, quizDate, DAILY_QUIZ_QUESTION_COUNT);

        Map<Long, Question> questionById = new HashMap<>();
        for (Question question : dailyQuizQuestionPickerRepository.findAllForDailyQuizByIds(selectedIds)) {
            questionById.put(question.getQuestionId(), question);
        }
        return selectedIds.stream().map(questionById::get).toList();
    }

    // 후보 전체를 한 바퀴 다 소진한 뒤에 다시 출제되도록 날짜를 블록 단위로 매핑한다.
    // 한 사이클(= 후보수 / 출제수 일)의 순서는 사이클 번호를 시드로 고정하므로,
    // 같은 날짜면 항상 같은 문항이 나오고 사이클 안에서는 하루도 겹치지 않는다.
    static List<Long> selectDailyQuizQuestionIds(List<Long> candidateIds, LocalDate quizDate, int count) {
        int blocksPerCycle = candidateIds.size() / count;
        if (blocksPerCycle == 0) {
            // 후보가 출제 수보다 적으면 그날 있는 만큼만 낸다.
            List<Long> remaining = new ArrayList<>(candidateIds);
            Collections.shuffle(remaining, new Random(quizDate.toEpochDay()));
            return List.copyOf(remaining);
        }

        long epochDay = quizDate.toEpochDay();
        long cycleIndex = Math.floorDiv(epochDay, blocksPerCycle);
        int blockIndex = (int) Math.floorMod(epochDay, blocksPerCycle);

        List<Long> shuffledIds = new ArrayList<>(candidateIds);
        Collections.shuffle(shuffledIds, new Random(cycleIndex));

        int from = blockIndex * count;
        return List.copyOf(shuffledIds.subList(from, from + count));
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
        String selectedAnswer = normalizeAnswer(answer.selectedAnswer());
        String correctAnswer = normalizeAnswer(question.getCorrectAnswer());
        return !selectedAnswer.isBlank() && selectedAnswer.equals(correctAnswer);
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
