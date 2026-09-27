package com.coremate.backend.weakness.dto.ai;

import com.coremate.backend.weakness.dto.type.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// AI 서버가 반환하는 약점 분석 해석. 와이어 포맷은 snake_case.
public record AiWeaknessAnalysisResponse(String overview,
        @JsonProperty("concept_analyses") List<ConceptAnalysis> conceptAnalyses,
        CallMeta meta) {

    /**
     * 실제 호출에 쓰인 모델명과 토큰 사용량. ai_call_logs 적재용이며 사용자 화면에는 쓰지 않는다.
     * 프로바이더가 값을 주지 않으면 각 필드는 null 이다(추정치를 만들지 않는다).
     */
    public record CallMeta(
            String model,
            @JsonProperty("input_tokens") Integer inputTokens,
            @JsonProperty("cached_tokens") Integer cachedTokens,
            @JsonProperty("output_tokens") Integer outputTokens,
            @JsonProperty("reasoning_tokens") Integer reasoningTokens,
            @JsonProperty("retry_count") Integer retryCount) {}

    public record ConceptAnalysis(
            @JsonProperty("concept_id") Long conceptId,
            @JsonProperty("analysis_status") AiAnalysisStatus analysisStatus,
            @JsonProperty("primary_type") WeaknessType primaryType,
            @JsonProperty("secondary_signals") List<SecondarySignal> secondarySignals,
            @JsonProperty("cause_code") CauseCode causeCode,
            @JsonProperty("confused_relation") String confusedRelation,
            String diagnosis,
            @JsonProperty("likely_cause") String likelyCause,
            @JsonProperty("concept_tip") String conceptTip,
            @JsonProperty("evidence_refs") List<String> evidenceRefs) {}
}
