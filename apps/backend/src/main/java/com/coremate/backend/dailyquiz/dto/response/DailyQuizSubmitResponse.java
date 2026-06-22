package com.coremate.backend.dailyquiz.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "오늘의 CS 퀴즈 제출 결과 응답 DTO")
public record DailyQuizSubmitResponse(
        @Schema(description = "생성된 오늘의 퀴즈 풀이 세션 ID", example = "9101")
        Long solveSessionId,

        @Schema(description = "오늘의 CS 퀴즈 세트 ID", example = "2026051101")
        Long dailyQuizSetId,

        @Schema(description = "퀴즈 날짜", example = "2026-05-11")
        LocalDate quizDate,

        @Schema(description = "총 문제 수", example = "5")
        Integer totalQuestionCount,

        @Schema(description = "정답 수", example = "4")
        Integer correctCount,

        @Schema(description = "오답 수", example = "1")
        Integer wrongCount,

        @Schema(description = "전체 풀이 시간 초", example = "180")
        Integer totalTimeSpentSec,

        @Schema(description = "출석 처리 여부", example = "true")
        Boolean attendanceProcessed,

        @Schema(description = "지급 보상")
        DailyQuizRewardResponse reward
) {
}
