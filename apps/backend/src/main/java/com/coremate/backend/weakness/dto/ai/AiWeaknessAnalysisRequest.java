package com.coremate.backend.weakness.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// 백엔드가 AI 서버로 보내는 약점 분석 입력(압축 근거). 와이어 포맷은 snake_case.
public record AiWeaknessAnalysisRequest(Scope scope, Totals totals,
        @JsonProperty("weakness_candidates") List<Candidate> weaknessCandidates) {

    public record Scope(String subject) {}

    public record Totals(
            @JsonProperty("question_count") int questionCount,
            @JsonProperty("wrong_count") int wrongCount,
            @JsonProperty("slow_count") int slowCount,
            @JsonProperty("hint_used_count") int hintUsedCount) {}

    public record Candidate(
            @JsonProperty("concept_id") Long conceptId,
            @JsonProperty("concept_name") String conceptName,
            @JsonProperty("concept_core") String conceptCore,
            @JsonProperty("weakness_score") int weaknessScore,
            @JsonProperty("evidence_strength") double evidenceStrength,
            @JsonProperty("type_separation") double typeSeparation,
            Stats stats,
            @JsonProperty("repeated_misconceptions") List<RepeatedMisconception> repeatedMisconceptions,
            List<Evidence> evidence) {}

    public record Stats(
            @JsonProperty("wrong_rate") double wrongRate,
            @JsonProperty("slow_rate") double slowRate,
            @JsonProperty("hint_rate") double hintRate) {}

    public record RepeatedMisconception(String code, int count) {}

    public record Evidence(String ref, String type,
            @JsonProperty("skill_type") String skillType,
            @JsonProperty("misconception_code") String misconceptionCode,
            @JsonProperty("time_ratio") Double timeRatio) {}
}
