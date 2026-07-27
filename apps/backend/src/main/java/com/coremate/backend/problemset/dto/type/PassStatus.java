package com.coremate.backend.problemset.dto.type;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "합격 여부 (전 문제 정답 시 PASS)")
public enum PassStatus {

    @Schema(description = "모든 문제 정답")
    PASS,

    @Schema(description = "일부 오답")
    FAIL
}
