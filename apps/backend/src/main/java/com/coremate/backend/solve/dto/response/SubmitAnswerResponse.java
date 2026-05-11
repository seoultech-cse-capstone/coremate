package com.coremate.backend.solve.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "답안 제출 결과 응답 DTO")
public record SubmitAnswerResponse(
        @Schema(description = "풀이 로그 ID", example = "10001")
        Long solveLogId,

        @Schema(description = "문제 ID", example = "5001")
        Long questionId,

        @Schema(description = "사용자가 제출한 답", example = "B")
        String selectedAnswer,

        @Schema(description = "정답", example = "B")
        String correctAnswer,

        @Schema(description = "정답 여부", example = "true")
        Boolean isCorrect,

        @Schema(description = "문제 해설")
        String explanationText,

        @Schema(description = "다음 문제 순서. 다음 문제가 없으면 null", example = "2")
        Integer nextQuestionOrder,

        @Schema(description = "세션 내 모든 문제 제출 완료 여부", example = "false")
        Boolean isSessionAnswerCompleted
) {
}
