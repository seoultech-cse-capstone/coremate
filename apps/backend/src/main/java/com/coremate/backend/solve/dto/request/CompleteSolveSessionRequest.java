package com.coremate.backend.solve.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(description = "풀이 세션 완료 요청 DTO")
public record CompleteSolveSessionRequest(
        @Min(0)
        @Schema(description = "전체 풀이 시간 초", example = "420")
        Integer totalTimeSpentSec
) {
}
