package com.coremate.backend.reward.entity.type;

// 광고 시청 보상 처리 결과. FAILED 는 일일 한도 초과처럼 지급하지 않은 경우이고,
// 구체적인 사유는 같은 건의 reward_logs.skip_reason 에 남는다.
public enum AdRewardStatus {
    REWARDED,
    FAILED
}
