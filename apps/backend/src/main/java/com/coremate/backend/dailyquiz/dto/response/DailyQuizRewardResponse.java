package com.coremate.backend.dailyquiz.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "오늘의 CS 퀴즈 완료 보상 응답 DTO")
public record DailyQuizRewardResponse(
        @Schema(description = "지급 XP", example = "10")
        Integer xpAmount,

        @Schema(description = "지급 코인", example = "5")
        Integer coinAmount,

        @Schema(description = "랜덤박스 ID. 지급되지 않으면 null", example = "1")
        Long randomBoxId,

        @Schema(description = "랜덤박스명", example = "하급 랜덤박스")
        String randomBoxName,

        @Schema(description = "출석 처리 여부", example = "true")
        Boolean attendanceProcessed,

        @Schema(description = "보상 수령 여부", example = "true")
        Boolean rewardClaimed
) {
}
