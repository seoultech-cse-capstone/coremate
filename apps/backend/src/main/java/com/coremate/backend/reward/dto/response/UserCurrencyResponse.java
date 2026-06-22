package com.coremate.backend.reward.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "사용자 재화 응답")
public record UserCurrencyResponse(
        @Schema(example = "42") Long userId,
        @Schema(example = "350") Integer coin,
        @Schema(example = "2026-05-12T09:10:11") LocalDateTime updatedAt
) {
}
