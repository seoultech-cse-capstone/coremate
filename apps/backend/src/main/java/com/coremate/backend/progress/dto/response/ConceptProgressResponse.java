package com.coremate.backend.progress.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "하위 개념 완료 상태 응답 DTO")
public record ConceptProgressResponse(
        @Schema(description = "개념 진행 상태 ID", example = "8001")
        Long userConceptProgressId,

        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "개념 ID", example = "1001")
        Long conceptId,

        @Schema(description = "개념명", example = "완전 이진 트리")
        String conceptName,

        @Schema(description = "중분류 ID", example = "101")
        Long categoryLv2Id,

        @Schema(description = "완료 여부", example = "true")
        Boolean isCompleted,

        @Schema(description = "완료 기준 풀이 회차 ID", example = "9001")
        Long completedSolveSessionId,

        @Schema(description = "완료 시간")
        LocalDateTime completedAt
) {
}
