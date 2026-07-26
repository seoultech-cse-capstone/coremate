package com.coremate.backend.reward.entity.type;

// reward_logs에 남기는 보상 미지급 사유.
public enum RewardSkipReason {
    LOW_CORRECT_RATE,
    ALREADY_REWARDED,
    DAILY_LIMIT_EXCEEDED,
    POLICY_NOT_FOUND,
    INSUFFICIENT_COIN,
    INVALID_REWARD_AMOUNT
}
