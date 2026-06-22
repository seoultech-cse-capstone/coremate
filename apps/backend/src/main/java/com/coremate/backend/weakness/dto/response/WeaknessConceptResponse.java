package com.coremate.backend.weakness.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "AI가 약점으로 판단한 개념별 분석 결과 DTO")
public record WeaknessConceptResponse(
        @Schema(description = "약점 개념 분석 ID", example = "30001")
        Long analysisConceptId,

        @Schema(description = "개념 ID", example = "1001")
        Long conceptId,

        @Schema(description = "개념명", example = "완전 이진 트리")
        String conceptName,

        @Schema(description = "약점 강도 1~5", example = "4")
        Integer weaknessLevel,

        @Schema(description = "약점으로 판단한 이유")
        String weaknessReason,

        @Schema(description = "관련 풀이 기록 요약")
        String evidenceSummary,

        @Schema(description = "해설보다 더 자세한 개념 설명")
        String detailedExplanationText,

        @Schema(description = "이후 복습 방향")
        String recommendationText,

        @Schema(description = "약점 판단 근거 로그 목록")
        List<WeaknessEvidenceLogResponse> evidenceLogs
) {
}
