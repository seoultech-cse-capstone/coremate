package com.coremate.backend.reward.dto.response;

import com.coremate.backend.reward.entity.type.RewardSkipReason;
import com.coremate.backend.reward.entity.type.RewardSource;
import com.coremate.backend.reward.entity.type.RewardStatus;
import com.coremate.backend.reward.entity.type.RewardTargetType;
import com.coremate.backend.reward.entity.type.RewardType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "보상 로그 응답")
public record RewardLogResponse(
        @Schema(example = "1") Long rewardLogId,
        @Schema(example = "42") Long userId,
        @Schema(example = "CONCEPT_PRACTICE_XP") RewardType rewardType,
        @Schema(example = "SOLVE_SESSION") RewardSource rewardSource,
        @Schema(example = "10") Long sourceId,
        @Schema(example = "CONCEPT") RewardTargetType targetType,
        @Schema(example = "1001") Long targetId,
        @Schema(example = "9001") Long solveSessionId,
        @Schema(example = "5") Integer xpAmount,
        @Schema(example = "50") Integer coinAmount,
        @Schema(example = "1") Long itemId,
        @Schema(example = "2") Long randomBoxId,
        @Schema(example = "GRANTED") RewardStatus rewardStatus,
        @Schema(example = "DAILY_LIMIT_EXCEEDED") RewardSkipReason skipReason,
        @Schema(example = "2026-05-12T09:10:11") LocalDateTime createdAt
) {
}
