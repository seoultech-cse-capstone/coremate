package com.coremate.backend.solve.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "비회원 문제별 정답 확인 결과 DTO")
public record GuestAnswerResultResponse(
        @Schema(description = "문제 ID", example = "5001")
        Long questionId,

        @Schema(description = "선택한 답", example = "B")
        String selectedAnswer,

        @Schema(description = "정답", example = "B")
        String correctAnswer,

        @Schema(description = "정답 여부", example = "true")
        Boolean isCorrect,

        @Schema(description = "문제 해설")
        String explanationText
) {
}
