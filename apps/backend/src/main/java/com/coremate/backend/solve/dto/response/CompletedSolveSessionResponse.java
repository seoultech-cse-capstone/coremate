package com.coremate.backend.solve.dto.response;

import com.coremate.backend.solve.dto.type.SolveSessionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "완료된 문제집 목록 응답 DTO")
public record CompletedSolveSessionResponse(
        @Schema(description = "풀이 세션 ID", example = "9001")
        Long solveSessionId,

        @Schema(description = "제목", example = "완전 이진 트리 개념 문제집")
        String title,

        @Schema(description = "풀이 유형", example = "CONCEPT_PRACTICE")
        SolveSessionType sessionType,

        @Schema(description = "과목 ID", example = "1")
        Long subjectId,

        @Schema(description = "과목명", example = "자료구조")
        String subjectName,

        @Schema(description = "대분류 ID", example = "10")
        Long categoryLv1Id,

        @Schema(description = "대분류명", example = "트리")
        String categoryLv1Name,

        @Schema(description = "중분류 ID", example = "101")
        Long categoryLv2Id,

        @Schema(description = "중분류명", example = "이진 트리")
        String categoryLv2Name,

        @Schema(description = "개념 ID", example = "1001")
        Long conceptId,

        @Schema(description = "개념명", example = "완전 이진 트리")
        String conceptName,

        @Schema(description = "총 문제 수", example = "3")
        Integer totalQuestionCount,

        @Schema(description = "틀린 문제 수", example = "1")
        Integer wrongCount,

        @Schema(description = "완료 시간")
        LocalDateTime completedAt
) {
}
