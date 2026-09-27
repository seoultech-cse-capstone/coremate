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
        @Schema(description = "범위별 평생 1회 지급 여부", example = "false") Boolean firstRewardOnly,
        @Schema(description = "같은 범위로 하루 1회만 지급하는지 여부", example = "false") Boolean scopeDailyOnce,
        @Schema(example = "PER_SESSION") RewardScope rewardScope,
        @Schema(example = "개념 문제집 완료 XP") String description
) {
}
