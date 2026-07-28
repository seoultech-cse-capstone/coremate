package com.coremate.backend.weakness.service.model;

import java.util.List;

/**
 * 결정론 스코어링 결과(내부 모델). sufficient=false 면 근거 부족으로 AI 미호출(INSUFFICIENT_EVIDENCE).
 */
public record WeaknessScoringResult(
        List<ConceptCandidate> candidates,
        int totalQuestionCount,
        int totalWrongCount,
        int totalLongTimeCount,
        int totalHintUsedCount,
        boolean sufficient
) {
}
