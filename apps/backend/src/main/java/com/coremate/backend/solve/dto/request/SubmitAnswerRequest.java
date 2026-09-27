package com.coremate.backend.solve.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "문제별 답안 제출 요청 DTO")
public record SubmitAnswerRequest(
        @NotNull
        @Schema(description = "문제 ID", example = "5001", requiredMode = Schema.RequiredMode.REQUIRED)
        Long questionId,

        @Schema(description = "사용자가 최종 선택한 답", example = "B")
        String selectedAnswer,

        @Min(0)
        @Schema(description = "해당 문제 풀이 시간 초", example = "38")
        Integer timeSpentSec,

        @Schema(description = "힌트 사용 여부", example = "true")
        Boolean hintUsed,

        @Min(0)
        @Schema(description = "힌트 사용 횟수", example = "1")
        Integer hintCount,

        @Min(0)
        @Schema(description = "답 변경 횟수", example = "1")
        Integer optionChangeCount,

        @Schema(description = "답을 한 번이라도 바꿨는지 여부", example = "true")
        Boolean isAnswerChanged,

        @Schema(description = "풀이 당시 즐겨찾기 여부", example = "false")
        Boolean isBookmarked
) {
}
