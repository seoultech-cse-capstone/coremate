package com.coremate.backend.dailyquiz.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "오늘의 CS 퀴즈 세트 조회 응답 DTO")
public record DailyQuizSetResponse(
        @Schema(description = "오늘의 CS 퀴즈 세트 ID", example = "2026051101")
        Long dailyQuizSetId,

        @Schema(description = "퀴즈 날짜", example = "2026-05-11")
        LocalDate quizDate,

        @Schema(description = "총 문제 수", example = "5")
        Integer totalQuestionCount,

        @Schema(description = "오늘 이미 제출했는지 여부", example = "false")
        Boolean solvedToday,

        @Schema(description = "문제 목록")
        List<DailyQuizQuestionResponse> questions
) {
}
