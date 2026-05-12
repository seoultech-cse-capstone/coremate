package com.coremate.backend.dailyquiz.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "오늘의 CS 퀴즈 전체 제출 요청 DTO")
public record SubmitDailyQuizRequest(
        @Valid
        @NotEmpty
        @Schema(description = "제출 답안 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        List<SubmitDailyQuizAnswerRequest> answers,

        @Min(0)
        @Schema(description = "전체 풀이 시간 초", example = "180")
        Integer totalTimeSpentSec
) {
}
