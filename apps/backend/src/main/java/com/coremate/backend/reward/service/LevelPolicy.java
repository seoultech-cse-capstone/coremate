package com.coremate.backend.reward.service;

import java.util.List;

/**
 * 레벨 곡선과 레벨업 보상 구간 (2026-07-30 확정).
 *
 * <p>레벨 n에서 n+1로 가는 데 필요한 XP = {@code BASE_XP + STEP_XP * (n - 1)}.
 * 즉 1→2레벨 100 XP, 2→3레벨 150 XP처럼 레벨마다 50 XP씩 더 든다.
 * 레벨 L 달성에 필요한 누적 XP는 {@code 25L² + 25L - 50}이다.
 *
 * <p>지급량과 달리 이 곡선은 정책 테이블에 담을 컬럼이 없어 코드 상수로 둔다.
 * 사용자 테스트 후 조정될 가능성이 있으므로 한곳에 모아 두었다.
 */
public final class LevelPolicy {

    private static final int BASE_XP = 100;
    private static final int STEP_XP = 50;
    static final int MAX_LEVEL = 50;

    /** 보상이 걸린 레벨 구간. 이 외 레벨은 보상 없이 레벨만 오른다. */
    private static final List<Integer> REWARD_LEVELS = List.of(5, 10, 20, 30, 40, 50);

    private LevelPolicy() {
    }

    /**
     * 누적 XP로 레벨을 구한다. 최소 1레벨, 최대 {@link #MAX_LEVEL}.
     *
     * <p>레벨마다 요구량이 달라지므로 1레벨부터 누적 요구량을 더해 올라간다.
     * 상한이 50이라 최대 49회 반복이고, XP 지급 시에만 호출되므로 비용은 무시할 수준이다.
     */
    public static int levelOf(int totalXp) {
        int level = 1;
        int required = BASE_XP;
        int accumulated = 0;
        while (level < MAX_LEVEL && totalXp >= accumulated + required) {
            accumulated += required;
            required += STEP_XP;
            level++;
        }
        return level;
    }

    /**
     * 레벨이 {@code before}에서 {@code after}로 오르면서 새로 넘어선 보상 구간들.
     *
     * <p>한 번에 여러 레벨이 오르면 그 사이의 보상 구간을 모두 돌려준다.
     * 레벨이 오르지 않았거나 내려갔으면 빈 목록이다.
     */
    public static List<Integer> rewardLevelsBetween(int before, int after) {
        if (after <= before) {
            return List.of();
        }
        return REWARD_LEVELS.stream()
                .filter(level -> level > before && level <= after)
                .toList();
    }
}
