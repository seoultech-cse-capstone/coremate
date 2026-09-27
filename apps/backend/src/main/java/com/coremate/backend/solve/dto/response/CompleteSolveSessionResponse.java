package com.coremate.backend.solve.dto.response;

import com.coremate.backend.reward.entity.type.RewardSkipReason;
import com.coremate.backend.solve.dto.type.SolveSessionStatus;
import com.coremate.backend.solve.dto.type.SolveSessionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "풀이 완료 결과 응답 DTO")
public record CompleteSolveSessionResponse(
        @Schema(description = "풀이 세션 ID", example = "9001")
        Long solveSessionId,

        @Schema(description = "문제집 제목", example = "완전 이진 트리 개념 문제집")
        String title,

        @Schema(description = "풀이 유형", example = "CONCEPT_PRACTICE")
        SolveSessionType sessionType,

        @Schema(description = "총 문제 수", example = "3")
        Integer totalQuestionCount,

        @Schema(description = "정답 수", example = "2")
        Integer correctCount,

        @Schema(description = "오답 수", example = "1")
        Integer wrongCount,

        @Schema(description = "전체 풀이 시간 초", example = "420")
        Integer totalTimeSpentSec,

        @Schema(description = "풀이 상태", example = "COMPLETED")
        SolveSessionStatus status,

        @Schema(description = "완료 시간")
        LocalDateTime completedAt,

        @Schema(description = "이번 회차로 지급된 XP. 일일 한도를 넘겼으면 0", example = "10")
        Integer earnedXp,

        @Schema(description = "이번 회차로 지급된 코인. 조건 미충족이면 0", example = "0")
        Integer earnedCoin,

        @Schema(description = "회차 정답률(%). 코인 지급 기준과 비교되는 값", example = "66.67")
        BigDecimal correctRate,

        @Schema(description = "XP 미지급 사유. 지급됐으면 null",
                example = "DAILY_LIMIT_EXCEEDED")
        RewardSkipReason xpSkipReason,

        @Schema(description = "코인 미지급 사유. 지급됐거나 대상이 아니면 null",
                example = "LOW_CORRECT_RATE")
        RewardSkipReason coinSkipReason
) {
}
