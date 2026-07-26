package com.coremate.backend.weakness.dto.ai;

import com.coremate.backend.weakness.dto.type.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// AI 서버가 반환하는 약점 분석 해석. 와이어 포맷은 snake_case.
public record AiWeaknessAnalysisResponse(String overview,
        @JsonProperty("concept_analyses") List<ConceptAnalysis> conceptAnalyses) {

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
