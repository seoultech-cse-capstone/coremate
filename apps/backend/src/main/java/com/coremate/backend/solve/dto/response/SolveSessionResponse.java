package com.coremate.backend.solve.dto.response;

import com.coremate.backend.solve.dto.type.SolveSessionStatus;
import com.coremate.backend.solve.dto.type.SolveSessionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "풀이 세션 생성 응답 DTO")
public record SolveSessionResponse(
        @Schema(description = "풀이 세션 ID", example = "9001")
        Long solveSessionId,

        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "풀이 세션 유형", example = "CONCEPT_PRACTICE")
        SolveSessionType sessionType,

        @Schema(description = "대상 ID", example = "1001")
        Long targetId,

        @Schema(description = "문제집 제목", example = "완전 이진 트리 개념 문제집")
        String title,

        @Schema(description = "총 문제 수", example = "3")
        Integer totalQuestionCount,

        @Schema(description = "풀이 상태", example = "IN_PROGRESS")
        SolveSessionStatus status,

        @Schema(description = "풀이 시작 시간")
        LocalDateTime startedAt,

        @Schema(description = "이번 회차에 출제된 문제 ID 목록", example = "[5001, 5002, 5003]")
        List<Long> questionIds
) {
}
