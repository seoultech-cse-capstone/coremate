package com.coremate.backend.progress.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "복습탭 개념 카운팅 응답 DTO")
public record ConceptCountSummaryResponse(
        @Schema(description = "전체 개념 수(활성 개념 기준)", example = "500")
        long totalConceptCount,

        @Schema(description = "풀었어요: 합격(PASS, 전 문제 정답)한 개념 문제집 세션이 있는 개념 수", example = "100")
        long solvedConceptCount,

        @Schema(description = "아직 못풀었어요: 전체 - 풀었어요", example = "400")
        long unsolvedConceptCount
) {
}
