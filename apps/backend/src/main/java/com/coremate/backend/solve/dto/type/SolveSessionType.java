package com.coremate.backend.solve.dto.type;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "풀이 세션 유형")
public enum SolveSessionType {
    @Schema(description = "하위 개념 단위 개념 문제집")
    CONCEPT_PRACTICE,

    @Schema(description = "대분류 기준 챕터 모의고사")
    CATEGORY_LV1_MOCK,

    @Schema(description = "중분류 기준 챕터 모의고사")
    CATEGORY_LV2_MOCK,

    @Schema(description = "과목 전체 모의고사")
    SUBJECT_MOCK,

    @Schema(description = "오늘의 CS 퀴즈")
    DAILY_QUIZ
}
