package com.coremate.backend.weakness.service;

import com.coremate.backend.content.entity.Subject;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.solve.dto.type.SolveSessionStatus;
import com.coremate.backend.solve.dto.type.SolveSessionType;
import com.coremate.backend.solve.entity.SolveSession;
import com.coremate.backend.solve.repository.SolveSessionRepository;
import com.coremate.backend.weakness.config.WeaknessAnalysisProperties;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 약점분석 대상 선택 검증: 소유권 · 완료 여부 · 개수(1~5) · 모의고사만(개념 제외) · 같은 과목 · 중복.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeaknessSelectionValidator {

    private static final Set<SolveSessionType> MOCK_TYPES = Set.of(
            SolveSessionType.CATEGORY_LV1_MOCK,
            SolveSessionType.CATEGORY_LV2_MOCK,
            SolveSessionType.SUBJECT_MOCK);

    private final SolveSessionRepository solveSessionRepository;
    private final WeaknessAnalysisProperties props;

    public List<SolveSession> validateAndLoad(Long userId, List<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            throw new CoreMateException(ErrorCode.WEAKNESS_SELECTION_EMPTY);
        }
        List<Long> distinct = sessionIds.stream().distinct().toList();
        if (distinct.size() != sessionIds.size()) {
            throw new CoreMateException(ErrorCode.WEAKNESS_DUPLICATE_SESSION);
        }
        if (distinct.size() < props.getMinSelection()) {
            throw new CoreMateException(ErrorCode.WEAKNESS_SELECTION_EMPTY);
        }
        if (distinct.size() > props.getMaxSelection()) {
            throw new CoreMateException(ErrorCode.WEAKNESS_SELECTION_TOO_MANY);
        }

        List<SolveSession> sessions = solveSessionRepository.findBySolveSessionIdInAndUserId(distinct, userId);
        if (sessions.size() != distinct.size()) {
            // 소유하지 않았거나 존재하지 않는 회차 포함
            throw new CoreMateException(ErrorCode.WEAKNESS_SESSION_NOT_FOUND);
        }

        for (SolveSession session : sessions) {
            if (session.getStatus() != SolveSessionStatus.COMPLETED) {
                throw new CoreMateException(ErrorCode.WEAKNESS_SESSION_NOT_COMPLETED);
            }
            if (!MOCK_TYPES.contains(session.getSessionType())) {
                throw new CoreMateException(ErrorCode.WEAKNESS_SESSION_NOT_MOCK);
            }
        }

        long distinctSubjects = sessions.stream().map(this::resolveSubjectId).distinct().count();
        if (distinctSubjects > 1) {
            throw new CoreMateException(ErrorCode.WEAKNESS_MIXED_SUBJECT);
        }
        return sessions;
    }

    private Long resolveSubjectId(SolveSession session) {
        if (session.getSubject() != null) {
            return session.getSubject().getSubjectId();
        }
        if (session.getCategoryLv2() != null) {
            Subject subject = session.getCategoryLv2().getCategoryLv1().getSubject();
            return subject != null ? subject.getSubjectId() : null;
        }
        if (session.getCategoryLv1() != null) {
            Subject subject = session.getCategoryLv1().getSubject();
            return subject != null ? subject.getSubjectId() : null;
        }
        return null;
    }
}
