package com.coremate.backend.reward.entity.type;

// reward_logs에 남기는 보상 미지급 사유.
public enum RewardSkipReason {
    LOW_CORRECT_RATE,
    ALREADY_REWARDED,
    // 같은 범위(대분류/중분류/과목)로 오늘 이미 받은 경우. ALREADY_REWARDED(평생 1회)와 달리 내일 다시 받을 수 있다.
    ALREADY_REWARDED_TODAY,
    DAILY_LIMIT_EXCEEDED,
    POLICY_NOT_FOUND,
    INSUFFICIENT_COIN,
    INVALID_REWARD_AMOUNT
}
