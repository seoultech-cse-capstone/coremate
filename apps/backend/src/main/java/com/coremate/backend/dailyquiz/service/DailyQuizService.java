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
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyQuizService {

    private static final int DAILY_QUIZ_QUESTION_COUNT = 6;
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    // 오늘의 퀴즈 전역 세트 캐시. 날짜를 키에 넣으므로 자정이 지나면 자동으로 새 키가 되어
    // 별도 무효화가 필요 없다. TTL 은 메모리 회수용 안전장치일 뿐이라 하루보다 넉넉히 잡는다.
    private static final String DAILY_QUIZ_SET_CACHE_KEY_PREFIX = "daily-quiz:set:";
    private static final Duration DAILY_QUIZ_SET_CACHE_TTL = Duration.ofHours(26);

    private final DailyQuizSetRepository dailyQuizSetRepository;
    private final DailyQuizQuestionPickerRepository dailyQuizQuestionPickerRepository;
    private final DailyQuizAnswerLogRepository dailyQuizAnswerLogRepository;
    private final AttendanceService attendanceService;
    private final DailyQuizSetInitializer dailyQuizSetInitializer;
    private final StringRedisTemplate stringRedisTemplate;

    // 오늘의 퀴즈 세트 생성을 한 번에 하나만 하도록 묶는다. 인스턴스가 여러 개면 이 잠금이
    // 닿지 않지만, 그때는 유니크 제약에 걸린 쪽이 createIfAbsent 안에서 예외를 삼키고
    // 이미 만들어진 세트를 읽어 간다. 즉 이 잠금은 같은 인스턴스 안의 흔한 경합을 없애는 용도다.
    private static final Object DAILY_QUIZ_CREATION_LOCK = new Object();
    private final ObjectMapper objectMapper;

    // 오늘 날짜의 퀴즈 세트를 조회하고 없으면 승인된 문제 후보에서 생성한다.
    //
    // NOT_SUPPORTED 로 바깥 트랜잭션을 아예 열지 않는다. 이유가 두 가지다.
    //  ① 세트 생성은 DailyQuizSetInitializer 가 독립 트랜잭션으로 하고 여기서 다시 읽어야 하는데,
    //     바깥 트랜잭션이 있으면 스냅샷이 고정돼 방금 커밋된 세트를 못 본다.
    //  ② REQUIRES_NEW 는 바깥 트랜잭션을 "정지"시킬 뿐 커넥션은 붙잡고 있어서, 요청 하나가
    //     커넥션 두 개를 쥔다. 동시 요청이 몰리면 풀이 말라 CannotCreateTransactionException 이 난다.
    // 이 메서드는 조회와 위임뿐이라 트랜잭션이 필요 없다(클래스 기본값 readOnly 도 여기선 끈다).
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public DailyQuizSetResponse getTodayDailyQuiz(Long userId) {
        LocalDate today = LocalDate.now();
        // 세트(문항·선지 등)는 전 사용자 공통이고 당일 불변이라 캐시에서 가져온다.
        // 사용자마다 다른 solvedToday 만 매번 가벼운 출석 조회로 채워 응답을 조립한다.
        DailyQuizSetResponse cachedSet = getCachedDailyQuizSet(today);
        boolean solvedToday = attendanceService.hasAttended(userId, today);
        return withSolvedToday(cachedSet, solvedToday);
    }

    // 오늘의 전역 퀴즈 세트를 Redis 에서 읽고, 없으면 DB 에서 만들어 캐시에 채운다.
    // 캐시된 응답의 solvedToday 는 의미 없는 자리값(false)이며 호출 측에서 덮어쓴다.
    private DailyQuizSetResponse getCachedDailyQuizSet(LocalDate today) {
        String key = DAILY_QUIZ_SET_CACHE_KEY_PREFIX + today;
        DailyQuizSetResponse cached = readSetFromCache(key);
        if (cached != null) {
            return cached;
        }
        DailyQuizSetResponse set = toSetResponse(getOrCreateDailyQuizSet(today), false);
        writeSetToCache(key, set);
        return set;
    }

    private DailyQuizSetResponse readSetFromCache(String key) {
        try {
            String cached = stringRedisTemplate.opsForValue().get(key);
            if (cached == null) {
                return null;
            }
            return objectMapper.readValue(cached, DailyQuizSetResponse.class);
        } catch (Exception e) {
            // 캐시는 성능 최적화일 뿐이라, Redis 장애나 값 손상 시 조용히 DB 로 폴백한다.
            log.warn("오늘의 퀴즈 캐시 조회 실패, DB 로 폴백한다. key={}", key, e);
            return null;
        }
    }

    private void writeSetToCache(String key, DailyQuizSetResponse set) {
        try {
            stringRedisTemplate.opsForValue()
                    .set(key, objectMapper.writeValueAsString(set), DAILY_QUIZ_SET_CACHE_TTL);
        } catch (Exception e) {
            // 캐시 저장 실패는 응답에 영향을 주지 않는다.
            log.warn("오늘의 퀴즈 캐시 저장 실패. key={}", key, e);
        }
    }

    private DailyQuizSetResponse withSolvedToday(DailyQuizSetResponse set, boolean solvedToday) {
        return new DailyQuizSetResponse(
                set.dailyQuizSetId(),
                set.quizDate(),
                set.totalQuestionCount(),
                solvedToday,
                set.questions()
        );
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

        // 동시에 들어온 요청들이 각자 세트를 만들다 데드락 나던 자리다.
        //
        // 잠금을 트랜잭션 "바깥"에서 잡는 게 핵심이다. createIfAbsent 안에서 잠그면 프록시가
        // 트랜잭션을 먼저 열기 때문에, 기다리는 요청들이 커넥션을 하나씩 쥔 채 줄을 서서
        // 커넥션 풀이 말라 버린다(실제로 그렇게 만들었다가 전부 500 이 났다).
        // 여기서 잠그면 기다리는 쪽은 커넥션을 쓰지 않고, 이긴 쪽이 만든 뒤 곧바로 빠져나간다.
        synchronized (DAILY_QUIZ_CREATION_LOCK) {
            dailyQuizSetInitializer.createIfAbsent(quizDate, () -> buildDailyQuizEntries(quizDate));
        }

        List<DailyQuizSet> created = dailyQuizSetRepository.findByQuizDateOrderBySortOrderAsc(quizDate);
        if (created.isEmpty()) {
            throw new CoreMateException(ErrorCode.DAILY_QUIZ_NOT_FOUND);
        }
        return created;
    }

    private List<DailyQuizSet> buildDailyQuizEntries(LocalDate quizDate) {
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
        return newEntries;
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
