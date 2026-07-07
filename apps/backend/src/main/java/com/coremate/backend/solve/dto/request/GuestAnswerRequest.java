package com.coremate.backend.solve.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "비회원 최종 답안 제출 요청 DTO")
public record GuestAnswerRequest(
        @NotNull
        @Schema(description = "개념 ID (문제 범위 검증용)", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
        Long conceptId,

        @NotEmpty
        @Valid
        @Schema(description = "문제별 답안 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        List<AnswerItem> answers
) {
    @Schema(description = "문제별 답안")
    public record AnswerItem(
            @NotNull
            @Schema(description = "문제 ID", example = "5001", requiredMode = Schema.RequiredMode.REQUIRED)
            Long questionId,

            @NotBlank
            @Schema(description = "선택한 답", example = "B", requiredMode = Schema.RequiredMode.REQUIRED)
            String selectedAnswer
    ) {}
}
