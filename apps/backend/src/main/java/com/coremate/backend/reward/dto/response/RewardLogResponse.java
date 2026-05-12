package com.coremate.backend.reward.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "보상 로그 응답")
public record RewardLogResponse(
        @Schema(example = "1") Long rewardLogId,
        @Schema(example = "42") Long userId,
        @Schema(example = "DAILY_ATTENDANCE") String rewardSource,
        @Schema(example = "10") Long sourceId,
        @Schema(example = "5") Integer xpAmount,
        @Schema(example = "50") Integer coinAmount,
        @Schema(example = "1") Long itemId,
        @Schema(example = "2") Long randomBoxId,
        @Schema(example = "2026-05-12T09:10:11") LocalDateTime createdAt
) {
}
