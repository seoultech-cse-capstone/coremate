package com.coremate.backend.problemset.dto.response;

import com.coremate.backend.problemset.dto.type.ProblemSetType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "문제집 요약 응답 DTO")
public record ProblemSetSummaryResponse(
        @Schema(description = "문제집 유형", example = "CONCEPT_PRACTICE")
        ProblemSetType problemSetType,

        @Schema(description = "대상 유형", example = "CONCEPT")
        String targetType,

        @Schema(description = "대상 ID", example = "1001")
        Long targetId,

        @Schema(description = "문제집 제목", example = "완전 이진 트리 개념 문제집")
        String title,

        @Schema(description = "총 문제 수", example = "5")
        Integer totalQuestionCount,

        @Schema(description = "완료 여부", example = "true")
        Boolean isCompleted,

        @Schema(description = "완료 횟수", example = "2")
        Integer completedCount,

        @Schema(description = "마지막 완료 시간")
        LocalDateTime lastCompletedAt,

        @Schema(description = "마지막 완료 회차 기준 틀린 문제 수", example = "1")
        Integer wrongCount
) {
}
