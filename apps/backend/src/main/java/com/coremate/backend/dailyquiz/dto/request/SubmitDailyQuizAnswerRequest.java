package com.coremate.backend.dailyquiz.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "오늘의 CS 퀴즈 개별 문제 답안 DTO")
public record SubmitDailyQuizAnswerRequest(
        @NotNull
        @Schema(description = "문제 ID", example = "5001", requiredMode = Schema.RequiredMode.REQUIRED)
        Long questionId,

        @Schema(description = "선택 답", example = "B")
        String selectedAnswer,

        @Min(0)
        @Schema(description = "풀이 시간 초", example = "30")
        Integer timeSpentSec,

        @Schema(description = "힌트 사용 여부", example = "false")
        Boolean hintUsed,

        @Min(0)
        @Schema(description = "힌트 사용 횟수", example = "0")
        Integer hintCount,

        @Min(0)
        @Schema(description = "답 변경 횟수", example = "0")
        Integer optionChangeCount,

        @Schema(description = "답 변경 여부", example = "false")
        Boolean isAnswerChanged
) {
}
