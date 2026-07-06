package com.coremate.backend.reward.dto.response;

import com.coremate.backend.reward.entity.type.RewardKind;
import com.coremate.backend.reward.entity.type.RewardScope;
import com.coremate.backend.reward.entity.type.RewardType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "보상 정책 응답")
public record RewardPolicyResponse(
        @Schema(example = "1") Long rewardPolicyId,
        @Schema(example = "CONCEPT_PRACTICE_XP") RewardType rewardType,
        @Schema(example = "XP") RewardKind rewardKind,
        @Schema(example = "0") Integer coinAmount,
        @Schema(example = "10") Integer xpAmount,
        @Schema(example = "10") Integer dailyLimit,
        @Schema(example = "70.00") BigDecimal minCorrectRate,
        @Schema(example = "false") Boolean firstRewardOnly,
        @Schema(example = "PER_SESSION") RewardScope rewardScope,
        @Schema(example = "개념 문제집 완료 XP") String description
) {
}
