package com.coremate.backend.problemset.dto.type;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "문제 형식")
public enum QuestionFormat {
    @Schema(description = "객관식 4지선다")
    MCQ_4,

    @Schema(description = "OX 문제")
    OX,

    @Schema(description = "단답형")
    SHORT_ANSWER,

    @Schema(description = "다중 선택형")
    MULTI_SELECT
}
