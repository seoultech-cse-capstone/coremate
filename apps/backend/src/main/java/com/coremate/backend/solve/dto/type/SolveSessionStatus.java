package com.coremate.backend.solve.dto.type;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "풀이 세션 상태")
public enum SolveSessionStatus {
    @Schema(description = "진행 중")
    IN_PROGRESS,

    @Schema(description = "완료")
    COMPLETED,

    @Schema(description = "중단")
    ABANDONED
}
