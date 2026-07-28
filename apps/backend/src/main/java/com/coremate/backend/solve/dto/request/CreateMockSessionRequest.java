package com.coremate.backend.solve.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "모의고사 풀이 세션 시작 요청 DTO")
public record CreateMockSessionRequest(
        @NotNull
        @Schema(description = "대상 ID (대분류/중분류/과목의 ID)", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
        Long targetId
) {
}
