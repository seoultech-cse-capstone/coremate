package com.coremate.backend.solve.dto.response;

import com.coremate.backend.solve.dto.type.SolveSessionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "완료된 문제집 기록/해설 상세 응답 DTO")
public record SolveSessionReviewResponse(
        @Schema(description = "풀이 세션 ID", example = "9001")
        Long solveSessionId,

        @Schema(description = "제목", example = "완전 이진 트리 개념 문제집")
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

        @Schema(description = "완료 시간")
        LocalDateTime completedAt,

        @Schema(description = "문제별 풀이 기록")
        List<SolveLogReviewResponse> logs
) {
}
