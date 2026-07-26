package com.coremate.backend.weakness.service.model;

import com.coremate.backend.weakness.dto.response.WeaknessEvidenceLogResponse;
import com.coremate.backend.weakness.dto.type.WeaknessType;
import java.math.BigDecimal;
import java.util.List;

/**
 * 백엔드가 결정론적으로 산출한 약점 개념 후보(내부 모델).
 * backendCandidateType 은 AI 교차검증용 내부 값으로, 사용자 표시값이 아니다.
 */
public record ConceptCandidate(
        Long conceptId,
        String conceptName,
        String conceptCore,
        int questionCount,
        int wrongCount,
        int longTimeCount,
        int hintUsedCount,
        int weaknessScore,
        BigDecimal evidenceStrength,
        BigDecimal typeSeparation,
        WeaknessType backendCandidateType,
        List<WeaknessEvidenceLogResponse> evidence
) {
}
