package com.coremate.backend.solve.dto.response;

import com.coremate.backend.solve.dto.type.SolveSessionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "풀이 세션 재진입 응답 DTO")
public record ResumeSessionResponse(
        @Schema(description = "풀이 세션 ID", example = "9001")
        Long solveSessionId,

        @Schema(description = "문제집 제목", example = "완전 이진 트리 개념 문제집")
        String title,

        @Schema(description = "풀이 유형", example = "CONCEPT_PRACTICE")
        SolveSessionType sessionType,

        @Schema(description = "총 문제 수", example = "3")
        Integer totalQuestionCount,

        @Schema(description = "풀이 시작 시간")
        LocalDateTime startedAt,

        @Schema(description = "전체 문제 ID 목록 (순서 유지)", example = "[5001, 5002, 5003]")
        List<Long> questionIds,

        @Schema(description = "이미 제출한 문제 목록")
        List<AnsweredLog> answeredLogs
) {
    @Schema(description = "이미 제출한 문제 정보")
    public record AnsweredLog(
            @Schema(description = "문제 ID", example = "5001")
            Long questionId,

            @Schema(description = "문제 순서", example = "1")
            Integer questionOrder,

            @Schema(description = "제출한 답", example = "B")
            String selectedAnswer
    ) {}
}
