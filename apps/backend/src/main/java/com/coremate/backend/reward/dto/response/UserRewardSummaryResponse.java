package com.coremate.backend.reward.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 보상 요약 응답")
public record UserRewardSummaryResponse(
        @Schema(example = "42") Long userId,
        @Schema(example = "1200") Integer totalXp,
        @Schema(example = "5") Integer level,
        @Schema(example = "350") Integer coin
) {
}
