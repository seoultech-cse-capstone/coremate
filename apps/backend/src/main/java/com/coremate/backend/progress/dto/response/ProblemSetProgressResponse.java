package com.coremate.backend.progress.dto.response;

import com.coremate.backend.problemset.dto.type.ProblemSetType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "문제집별 완료 상태 응답 DTO")
public record ProblemSetProgressResponse(
        @Schema(description = "문제집 진행 상태 ID", example = "7001")
        Long userProblemSetProgressId,

        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "문제집 유형", example = "CONCEPT_PRACTICE")
        ProblemSetType problemSetType,

        @Schema(description = "대상 유형", example = "CONCEPT")
        String targetType,

        @Schema(description = "대상 ID", example = "1001")
        Long targetId,

        @Schema(description = "과목 ID", example = "1")
        Long subjectId,

        @Schema(description = "대분류 ID", example = "10")
        Long categoryLv1Id,

        @Schema(description = "중분류 ID", example = "101")
        Long categoryLv2Id,

        @Schema(description = "개념 ID", example = "1001")
        Long conceptId,

        @Schema(description = "마지막 완료 회차 ID", example = "9001")
        Long lastSolveSessionId,

        @Schema(description = "완료 여부", example = "true")
        Boolean isCompleted,

        @Schema(description = "완료 횟수", example = "2")
        Integer completedCount,

        @Schema(description = "마지막 완료 시간")
        LocalDateTime lastCompletedAt
) {
}
