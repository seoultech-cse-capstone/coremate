package com.coremate.backend.avatar.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("친구 방 포즈 경과 시간 라벨 (SCR-FRIEND-02)")
class RoomPoseElapsedTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 28, 12, 0);

    @Nested
    @DisplayName("경과 시간 구간")
    class Buckets {

        @ParameterizedTest(name = "{0}분 전이면 \"{1}\"")
        @CsvSource({
                "0,     방금 전부터",
                "1,     방금 전부터",
                "59,    방금 전부터",   // 1시간 직전
                "60,    1시간 전부터",   // 1시간 도달
                "119,   1시간 전부터",
                "120,   2시간 전부터",
                "1439,  23시간 전부터",  // 24시간 직전
                "1440,  1일 전부터",     // 24시간 도달
                "4320,  3일 전부터"
        })
        void labelByElapsedMinutes(long minutesAgo, String expected) {
            String label = RoomPoseElapsed.label(
                    RoomPose.SLEEP_BED, NOW.minusMinutes(minutesAgo), NOW);

            assertThat(label).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("표시하지 않는 경우")
    class Hidden {

        @Test
        @DisplayName("STAND 는 라벨이 없다")
        void standHasNoLabel() {
            assertThat(RoomPoseElapsed.label(RoomPose.STAND, NOW.minusDays(3), NOW)).isNull();
        }

        @Test
        @DisplayName("포즈가 없으면 라벨이 없다")
        void nullPoseHasNoLabel() {
            assertThat(RoomPoseElapsed.label(null, NOW.minusDays(3), NOW)).isNull();
        }

        // 포즈를 한 번도 바꾼 적 없는 사용자.
        @Test
        @DisplayName("시각이 없으면 라벨이 없다")
        void nullTimestampHasNoLabel() {
            assertThat(RoomPoseElapsed.label(RoomPose.SLEEP_BED, null, NOW)).isNull();
        }
    }

    @Nested
    @DisplayName("이상한 시각")
    class Edge {

        // 서버·DB 시계 오차로 미래 시각이 들어오면 "-1일 전부터" 같은 라벨이 나가면 안 된다.
        @Test
        @DisplayName("미래 시각이면 방금으로 본다")
        void futureTimestampFallsBackToJustNow() {
            assertThat(RoomPoseElapsed.label(RoomPose.SIT_DESK, NOW.plusHours(5), NOW))
                    .isEqualTo("방금 전부터");
        }
    }
}
