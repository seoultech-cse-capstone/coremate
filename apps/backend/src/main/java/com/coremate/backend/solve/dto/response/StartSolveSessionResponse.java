package com.coremate.backend.solve.dto.response;

import com.coremate.backend.solve.dto.type.SolveSessionStatus;
import com.coremate.backend.solve.dto.type.SolveSessionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "풀이 세션 시작 응답 DTO. 비회원은 sessionId/status/startedAt이 null이며 문제 내용이 포함된다.")
public record StartSolveSessionResponse(
        @Schema(description = "풀이 세션 ID. 비회원은 null", example = "9001")
        Long sessionId,

        @Schema(description = "문제집 제목", example = "완전 이진 트리 개념 문제집")
        String title,

        @Schema(description = "풀이 유형", example = "CONCEPT_PRACTICE")
        SolveSessionType sessionType,

        @Schema(description = "총 문제 수", example = "3")
        Integer totalQuestionCount,

        @Schema(description = "풀이 상태. 비회원은 null", example = "IN_PROGRESS")
        SolveSessionStatus status,

        @Schema(description = "풀이 시작 시간. 비회원은 null")
        LocalDateTime startedAt,

        @Schema(description = "문제 목록 (본문 및 선택지 포함)")
        List<QuestionSolveResponse> questions
) {
}
