package com.coremate.backend.problemset.dto.type;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "문제 유형")
public enum QuestionType {

    @Schema(description = "정의 확인형")
    DEFINITION("definition"),

    @Schema(description = "개념 비교형")
    COMPARISON("comparison"),

    @Schema(description = "오해 교정형")
    MISCONCEPTION("misconception"),

    @Schema(description = "예시 판단형")
    EXAMPLE("example"),

    @Schema(description = "개념 적용형")
    APPLICATION("application");

    private final String value;

    QuestionType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}