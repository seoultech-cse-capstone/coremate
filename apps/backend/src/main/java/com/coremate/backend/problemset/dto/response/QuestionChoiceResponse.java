package com.coremate.backend.problemset.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "문제 선택지 응답 DTO")
public record QuestionChoiceResponse(
        @Schema(description = "선택지 키", example = "A")
        String choiceKey,

        @Schema(description = "선택지 내용", example = "마지막 레벨을 제외한 모든 레벨이 채워져 있다.")
        String choiceText
) {
}
