package com.coremate.backend.dailyquiz.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "오늘의 CS 퀴즈 결과/해설 응답 DTO")
public record DailyQuizResultResponse(
        @Schema(description = "퀴즈 날짜", example = "2026-05-11")
        LocalDate quizDate,

        @Schema(description = "문항별 결과/해설 목록")
        List<DailyQuizResultQuestionResponse> questions
) {
}
