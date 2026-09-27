package com.coremate.backend.weakness.dto.type;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "약점 분석 범위")
public enum AnalysisScopeType {
    @Schema(description = "과목 전체 기준")
    SUBJECT,

    @Schema(description = "대분류 챕터 기준")
    CATEGORY_LV1,

    @Schema(description = "중분류 챕터 기준")
    CATEGORY_LV2,

    @Schema(description = "하위 개념 기준")
    CONCEPT
}
