package com.coremate.backend.problemset.dto.type;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "문제 유형")
public enum QuestionType {
    @Schema(description = "정의 확인형")
    DEFINITION,

    @Schema(description = "개념 비교형")
    COMPARISON,

    @Schema(description = "오해 교정형")
    MISCONCEPTION,

    @Schema(description = "예시 판단형")
    EXAMPLE,

    @Schema(description = "개념 적용형")
    APPLICATION
}
