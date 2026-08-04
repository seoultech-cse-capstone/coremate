package com.coremate.backend.weakness.service;

import com.coremate.backend.content.entity.CategoryLv1;
import com.coremate.backend.content.entity.CategoryLv2;
import com.coremate.backend.content.entity.Subject;
import com.coremate.backend.solve.dto.type.SolveSessionStatus;
import com.coremate.backend.solve.dto.type.SolveSessionType;
import com.coremate.backend.solve.entity.SolveLog;
import com.coremate.backend.solve.entity.SolveSession;
import com.coremate.backend.solve.repository.SolveLogRepository;
import com.coremate.backend.solve.repository.SolveSessionRepository;
import com.coremate.backend.weakness.config.WeaknessAnalysisProperties;
import com.coremate.backend.weakness.dto.response.EligibleSolveSessionResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 약점분석 대상 후보(완료된 모의고사 회차) 조회. 회차별 오답/오래걸림/힌트 수를 집계하고
 * 최소 근거 기준을 만족하는지(eligible) 표시한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeaknessEligibilityService {

    private static final List<SolveSessionType> MOCK_TYPES = List.of(
            SolveSessionType.CATEGORY_LV1_MOCK,
            SolveSessionType.CATEGORY_LV2_MOCK,
            SolveSessionType.SUBJECT_MOCK);

    private final SolveSessionRepository solveSessionRepository;
    private final SolveLogRepository solveLogRepository;
    private final WeaknessAnalysisProperties props;

    public List<EligibleSolveSessionResponse> getEligibleSessions(
            Long userId, Long subjectId, Long categoryLv1Id, Long categoryLv2Id, Long conceptId) {

        return solveSessionRepository
                .findByUserIdAndStatusAndSessionTypeInOrderByCompletedAtDesc(userId, SolveSessionStatus.COMPLETED, MOCK_TYPES)
                .stream()
                .filter(s -> subjectId == null || subjectId.equals(resolveSubjectId(s)))
                .filter(s -> categoryLv1Id == null
                        || (s.getCategoryLv1() != null && categoryLv1Id.equals(s.getCategoryLv1().getCategoryLv1Id())))
                .filter(s -> categoryLv2Id == null
                        || (s.getCategoryLv2() != null && categoryLv2Id.equals(s.getCategoryLv2().getCategoryLv2Id())))
                .map(this::toResponse)
                .toList();
    }

    private EligibleSolveSessionResponse toResponse(SolveSession session) {
        List<SolveLog> logs = solveLogRepository.findAllBySolveSessionOrderByQuestionOrder(session);
        int questionCount = logs.size();
        int wrongCount = (int) logs.stream().filter(l -> !l.isCorrect()).count();
        int longTimeCount = (int) logs.stream().filter(this::isLongTime).count();
        int hintUsedCount = (int) logs.stream().filter(SolveLog::isHintUsed).count();

        boolean eligible = questionCount >= props.getMinQuestionCount() && (wrongCount + longTimeCount + hintUsedCount) > 0;
        String warning = eligible ? null
                : (questionCount < props.getMinQuestionCount()
                ? "문항 수가 적어 분석 신뢰도가 낮습니다."
                : "약점 신호(오답/오래 걸림/힌트)가 부족합니다.");

        Subject subject = resolveSubject(session);
        CategoryLv1 lv1 = session.getCategoryLv1();
        CategoryLv2 lv2 = session.getCategoryLv2();

        return new EligibleSolveSessionResponse(
                session.getSolveSessionId(),
                session.getTitle(),
                session.getSessionType(),
                subject != null ? subject.getSubjectId() : null,
                subject != null ? subject.getSubjectName() : null,
                lv1 != null ? lv1.getCategoryLv1Id() : null,
                lv1 != null ? lv1.getCategoryLv1Name() : null,
                lv2 != null ? lv2.getCategoryLv2Id() : null,
                lv2 != null ? lv2.getCategoryLv2Name() : null,
                null,
                null,
                questionCount,
                wrongCount,
                longTimeCount,
                hintUsedCount,
                session.getCompletedAt(),
                eligible,
                warning);
    }

    private boolean isLongTime(SolveLog log) {
        return log.isCorrect()
                && log.getExpectedSolveSecSnapshot() != null
                && log.getExpectedSolveSecSnapshot() > 0
                && log.getTimeSpentSec() >= log.getExpectedSolveSecSnapshot() * props.getLongTimeRatio();
    }

    private Long resolveSubjectId(SolveSession session) {
        Subject subject = resolveSubject(session);
        return subject != null ? subject.getSubjectId() : null;
    }

    private Subject resolveSubject(SolveSession session) {
        if (session.getSubject() != null) {
            return session.getSubject();
        }
        if (session.getCategoryLv2() != null) {
            return session.getCategoryLv2().getCategoryLv1().getSubject();
        }
        if (session.getCategoryLv1() != null) {
            return session.getCategoryLv1().getSubject();
        }
        return null;
    }
}
