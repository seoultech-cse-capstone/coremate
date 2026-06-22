package com.coremate.backend.solve.dto.response;

import com.coremate.backend.solve.dto.type.SolveSessionStatus;
import com.coremate.backend.solve.dto.type.SolveSessionType;
import io.swagger.v3.oas.annotations.media.Schema;

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

        @Schema(description = "지급 XP. Swagger 더미 값", example = "20")
        Integer earnedXp,

        @Schema(description = "지급 코인. Swagger 더미 값", example = "10")
        Integer earnedCoin
) {
}
