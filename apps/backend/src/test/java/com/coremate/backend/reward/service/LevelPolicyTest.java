package com.coremate.backend.reward.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("레벨 정책 (2026-07-30 확정)")
class LevelPolicyTest {

    @Nested
    @DisplayName("누적 XP -> 레벨")
    class LevelOf {

        @ParameterizedTest(name = "누적 {0} XP 는 {1}레벨")
        @CsvSource({
                // 레벨 L 달성 누적 XP = 25L^2 + 25L - 50
                "0, 1",
                "99, 1",      // 2레벨 직전
                "100, 2",     // 2레벨 도달
                "249, 2",     // 3레벨 직전
                "250, 3",
                "449, 3",
                "450, 4",
                "699, 4",     // 5레벨 직전
                "700, 5",     // 첫 보상 구간
                "2699, 9",    // 10레벨 직전
                "2700, 10",
                "10450, 20",
                "23200, 30",
                "41950, 40",
                "63700, 50"
        })
        void 누적_XP로_레벨을_구한다(int totalXp, int expectedLevel) {
            assertThat(LevelPolicy.levelOf(totalXp)).isEqualTo(expectedLevel);
        }

        @Test
        @DisplayName("XP 가 아무리 많아도 상한 레벨을 넘지 않는다")
        void 상한을_넘지_않는다() {
            assertThat(LevelPolicy.levelOf(Integer.MAX_VALUE)).isEqualTo(LevelPolicy.MAX_LEVEL);
        }

        @Test
        @DisplayName("음수 XP 는 1레벨로 본다")
        void 음수는_1레벨() {
            assertThat(LevelPolicy.levelOf(-100)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("레벨업 보상 구간")
    class RewardLevels {

        @Test
        @DisplayName("보상 구간에 도달하면 그 구간을 돌려준다")
        void 보상_구간_도달() {
            assertThat(LevelPolicy.rewardLevelsBetween(4, 5)).containsExactly(5);
        }

        @Test
        @DisplayName("한 번에 여러 레벨이 오르면 넘어선 구간을 모두 돌려준다")
        void 여러_구간_동시_통과() {
            assertThat(LevelPolicy.rewardLevelsBetween(3, 12)).containsExactly(5, 10);
        }

        @Test
        @DisplayName("보상 구간이 아닌 레벨만 오르면 비어 있다")
        void 보상_없는_구간() {
            assertThat(LevelPolicy.rewardLevelsBetween(6, 9)).isEmpty();
        }

        @Test
        @DisplayName("레벨이 그대로거나 내려가면 비어 있다")
        void 레벨_상승이_없으면_비어_있다() {
            assertThat(LevelPolicy.rewardLevelsBetween(10, 10)).isEmpty();
            // 레벨 공식이 바뀌어 기존 레벨이 재계산돼 내려가는 경우에도 보상이 나가면 안 된다.
            assertThat(LevelPolicy.rewardLevelsBetween(13, 6)).isEmpty();
        }

        @Test
        @DisplayName("전 구간을 한 번에 통과하면 6개 구간을 모두 돌려준다")
        void 전_구간_통과() {
            assertThat(LevelPolicy.rewardLevelsBetween(1, 50))
                    .isEqualTo(List.of(5, 10, 20, 30, 40, 50));
        }
    }

    @Nested
    @DisplayName("도달한 보상 구간 (놓친 보상 정산용)")
    class RewardLevelsUpTo {

        @Test
        @DisplayName("도달한 레벨까지의 보상 구간을 모두 돌려준다")
        void 도달한_구간_전체() {
            assertThat(LevelPolicy.rewardLevelsUpTo(12)).containsExactly(5, 10);
        }

        @Test
        @DisplayName("보상 구간에 도달하지 못했으면 비어 있다")
        void 도달_전에는_비어_있다() {
            assertThat(LevelPolicy.rewardLevelsUpTo(4)).isEmpty();
        }

        @Test
        @DisplayName("경계 레벨은 포함한다")
        void 경계_포함() {
            assertThat(LevelPolicy.rewardLevelsUpTo(5)).containsExactly(5);
        }
    }
}
