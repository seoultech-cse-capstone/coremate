package com.coremate.backend.solve.service;

import com.coremate.backend.reward.entity.type.RewardSkipReason;

/**
 * 풀이 회차 완료로 실제 지급된 보상과, 지급되지 않았다면 그 사유.
 *
 * <p>XP와 코인은 서로 독립적으로 판정되므로 사유도 따로 담는다.
 * 예를 들어 XP 일일 한도를 소진한 뒤에 완료한 회차라도 정답률과 최초 보상 조건을
 * 만족하면 코인은 지급된다.
 */
public record SolveRewardResult(
        int earnedXp,
        int earnedCoin,
        RewardSkipReason xpSkipReason,
        RewardSkipReason coinSkipReason
) {

    static SolveRewardResult none() {
        return new SolveRewardResult(0, 0, null, null);
    }
}
