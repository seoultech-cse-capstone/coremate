package com.coremate.backend.reward.dto.response;

import com.coremate.backend.reward.entity.type.RewardType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "일일 보상 제한 응답")
public record DailyRewardLimitResponse(
        @Schema(example = "1") Long dailyRewardLimitId,
        @Schema(example = "42") Long userId,
        @Schema(example = "2026-05-12") LocalDate rewardDate,
        @Schema(example = "CONCEPT_PRACTICE_XP") RewardType rewardType,
        @Schema(example = "1") Integer rewardCount,
        @Schema(example = "10") Integer dailyLimit,
        @Schema(example = "2026-05-12T09:10:11") LocalDateTime updatedAt
) {
}
