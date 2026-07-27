package com.coremate.backend.content.dto.type;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "개념 문제집 풀이 상태")
public enum ConceptSolveStatus {

    @Schema(description = "아직 풀이한 적 없음")
    NOT_STARTED,

    @Schema(description = "진행 중인 풀이 세션 존재")
    IN_PROGRESS,

    @Schema(description = "완료된 풀이 기록 존재")
    COMPLETED
}
