package com.coremate.backend.weakness.service;

import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.solve.entity.SolveSession;
import com.coremate.backend.weakness.dto.request.CreateWeaknessAnalysisReportRequest;
import com.coremate.backend.weakness.dto.response.WeaknessAnalysisReportDetailResponse;
import com.coremate.backend.weakness.dto.response.WeaknessConceptResponse;
import com.coremate.backend.weakness.dto.type.AnalysisReportStatus;
import com.coremate.backend.weakness.service.model.ConceptCandidate;
import com.coremate.backend.weakness.service.model.WeaknessScoringResult;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 약점분석 생성 오케스트레이션 — Phase 1 은 검증 + 결정론 스코어링까지만 수행하고
 * 백엔드 후보를 "미리보기"로 반환한다(저장·코인 차감·AI 호출·교차검증은 Phase 3).
 * 근거 부족이면 INSUFFICIENT_EVIDENCE 로 종료(AI 미호출).
 */
@Service
@RequiredArgsConstructor
public class WeaknessAnalysisService {

    private final WeaknessSelectionValidator selectionValidator;
    private final WeaknessScoringService scoringService;

    @Transactional(readOnly = true)
    public WeaknessAnalysisReportDetailResponse createPreview(Long userId, CreateWeaknessAnalysisReportRequest request) {
        List<SolveSession> sessions = selectionValidator.validateAndLoad(userId, request.solveSessionIds());
        WeaknessScoringResult result = scoringService.score(sessions);

        if (!result.sufficient()) {
            throw new CoreMateException(ErrorCode.WEAKNESS_INSUFFICIENT_EVIDENCE);
        }

        List<WeaknessConceptResponse> concepts = result.candidates().stream()
                .map(this::toConceptResponse)
                .toList();

        String title = (request.title() == null || request.title().isBlank())
                ? "약점 분석 미리보기"
                : request.title();

        // Phase 1: 백엔드 결정론 결과만 채운 미리보기(비저장). AI 해석 필드는 Phase 3에서 채움.
        return new WeaknessAnalysisReportDetailResponse(
                null,
                userId,
                title,
                request.analysisScopeType(),
                null,
                null,
                null,
                "MARKDOWN",
                sessions.size(),
                result.totalQuestionCount(),
                result.totalWrongCount(),
                result.totalLongTimeCount(),
                result.totalHintUsedCount(),
                0,
                AnalysisReportStatus.GENERATING,
                LocalDateTime.now(),
                null,
                null,
                concepts);
    }

    private WeaknessConceptResponse toConceptResponse(ConceptCandidate candidate) {
        String evidenceSummary = String.format(
                "오답 %d개, 오래 걸린 문제 %d개, 힌트 사용 %d개",
                candidate.wrongCount(), candidate.longTimeCount(), candidate.hintUsedCount());
        return new WeaknessConceptResponse(
                null,
                candidate.conceptId(),
                candidate.conceptName(),
                candidate.weaknessScore(),
                candidate.evidenceStrength(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                evidenceSummary,
                null,
                null,
                candidate.evidence());
    }
}
