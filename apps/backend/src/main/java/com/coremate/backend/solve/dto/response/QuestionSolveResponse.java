package com.coremate.backend.solve.dto.response;

import com.coremate.backend.problemset.dto.response.QuestionChoiceResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "풀이용 문제 응답 DTO")
public record QuestionSolveResponse(
        @Schema(description = "문제 ID", example = "5001")
        Long questionId,

        @Schema(description = "문제 본문")
        String questionText,

        @Schema(description = "선택지 목록")
        List<QuestionChoiceResponse> choices
) {
}
