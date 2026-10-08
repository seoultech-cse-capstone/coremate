package com.coremate.backend.learninghistory.service;

import com.coremate.backend.learninghistory.dto.response.LearningHistoryResponse;
import com.coremate.backend.learninghistory.entity.UserLearningHistoryStats;
import com.coremate.backend.learninghistory.repository.UserLearningHistoryStatsRepository;
import com.coremate.backend.solve.dto.type.SolveSessionType;
import com.coremate.backend.solve.entity.SolveLog;
import com.coremate.backend.solve.entity.SolveSession;
import com.coremate.backend.solve.repository.SolveLogRepository;
import com.coremate.backend.solve.repository.SolveSessionRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 풀이 완료 시 사용자 Learning History 누적 지표를 갱신한다.
 * 완료 처리 트랜잭션(SolveSessionService.completeSolveSession) 안에서 호출된다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class LearningHistoryService {

    private final UserLearningHistoryStatsRepository statsRepository;
    private final SolveLogRepository solveLogRepository;
    private final SolveSessionRepository solveSessionRepository;

    /**
     * 사용자 Learning History 누적 지표를 조회한다. 기록이 없으면 모두 0으로 반환한다(행 생성하지 않음).
     */
    @Transactional(readOnly = true)
    public LearningHistoryResponse getStats(Long userId) {
        return statsRepository.findByUserId(userId)
                .map(s -> new LearningHistoryResponse(
                        s.getTotalMockSolveTimeSec(),
                        s.getConceptCorrectQuestionCount(),
                        s.getPassedConceptCount()))
                .orElse(new LearningHistoryResponse(0, 0, 0));
    }

    /**
     * 완료된 세션 유형에 따라 지표를 누적한다.
     * ① 모의고사: 풀이 시간 합산
     * ② 개념 문제집: 새로 맞힌 distinct 문제 수
     * ③ 개념 문제집: 최초 PASS 시 합격 개념 +1
     */
    public void accumulate(SolveSession session, List<SolveLog> logs, int correctCount) {
        Long userId = session.getUserId();
        SolveSessionType type = session.getSessionType();

        UserLearningHistoryStats stats = statsRepository.findByUserId(userId)
                .orElseGet(() -> statsRepository.save(UserLearningHistoryStats.initFor(userId)));

        // ① 모의고사 총 풀이 시간
        if (type.isMock()) {
            stats.addMockSolveTime(session.getTotalTimeSpentSec());
            return;
        }

        // 개념 문제집만 ②③ 대상
        if (type != SolveSessionType.CONCEPT_PRACTICE || session.getConcept() == null) {
            return;
        }

        Long conceptId = session.getConcept().getConceptId();
        Long sessionId = session.getSolveSessionId();

        // ② 새로 맞힌 distinct 문제 수 (이전에 맞힌 문제 제외)
        Set<Long> currentCorrectIds = logs.stream()
                .filter(SolveLog::isCorrect)
                .map(log -> log.getQuestion().getQuestionId())
                .collect(Collectors.toSet());

        if (!currentCorrectIds.isEmpty()) {
            Set<Long> alreadyCorrect = solveLogRepository
                    .findCorrectlyAnsweredQuestionIds(userId, currentCorrectIds, sessionId);
            int newlyCorrect = (int) currentCorrectIds.stream()
                    .filter(qid -> !alreadyCorrect.contains(qid))
                    .count();
            if (newlyCorrect > 0) {
                stats.addConceptCorrectQuestions(newlyCorrect);
            }
        }

        // ③ 최초 PASS 시 합격 개념 +1
        boolean isPass = session.getTotalQuestionCount() > 0
                && correctCount == session.getTotalQuestionCount();
        if (isPass && !solveSessionRepository.existsPassedConceptSession(userId, conceptId, sessionId)) {
            stats.increasePassedConcept();
        }
    }
}
