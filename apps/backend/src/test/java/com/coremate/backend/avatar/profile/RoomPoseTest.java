package com.coremate.backend.avatar.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.coremate.backend.avatar.room.UserRoomPlacement;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.user.entity.User;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("아바타 포즈 상태 (2026-08-27 확정)")
class RoomPoseTest {

    private static final LocalDateTime T1 = LocalDateTime.of(2026, 8, 25, 10, 0);
    private static final LocalDateTime T2 = LocalDateTime.of(2026, 8, 28, 10, 0);

    private UserAvatarProfile newProfile() {
        return new UserAvatarProfile(mock(User.class));
    }

    @Nested
    @DisplayName("저장하는 포즈는 3종이다")
    class Values {

        @Test
        @DisplayName("그리는 포즈는 4종이지만 저장값은 STAND/SIT_DESK/SLEEP_BED 뿐이다")
        void onlyThreeStoredValues() {
            assertThat(Arrays.stream(RoomPose.values()).map(Enum::name))
                    .containsExactlyInAnyOrder("STAND", "SIT_DESK", "SLEEP_BED");
        }

        @Test
        @DisplayName("STAND 만 가구를 특정하지 않는다")
        void onlyStandNeedsNoPlacement() {
            assertThat(RoomPose.STAND.requiresPlacement()).isFalse();
            assertThat(RoomPose.SIT_DESK.requiresPlacement()).isTrue();
            assertThat(RoomPose.SLEEP_BED.requiresPlacement()).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"STANDING", "SLEEP", "", "   "})
        @DisplayName("정의되지 않은 포즈 값은 예외 - EX-ITEM-008")
        void rejectsUnknown(String raw) {
            assertThatThrownBy(() -> RoomPose.from(raw)).isInstanceOf(CoreMateException.class);
        }
    }

    @Nested
    @DisplayName("시각은 포즈가 실제로 바뀔 때만 갱신한다")
    class Timestamp {

        @Test
        @DisplayName("포즈가 바뀌면 시각을 갱신한다")
        void updatesWhenPoseChanges() {
            UserAvatarProfile profile = newProfile();
            UserRoomPlacement bed = mock(UserRoomPlacement.class);

            profile.updateRoomPose(RoomPose.SLEEP_BED, bed, T1);

            assertThat(profile.getRoomPose()).isEqualTo(RoomPose.SLEEP_BED);
            assertThat(profile.getRoomPosePlacement()).isSameAs(bed);
            assertThat(profile.getRoomPoseUpdatedAt()).isEqualTo(T1);
        }

        // 이게 깨지면 친구 방의 "3일 전부터 자는 중"이 앱을 켤 때마다 "방금 전부터"로 초기화된다.
        @Test
        @DisplayName("같은 포즈를 다시 저장하면 시각이 그대로다")
        void keepsTimestampWhenPoseUnchanged() {
            UserAvatarProfile profile = newProfile();
            UserRoomPlacement bed = mock(UserRoomPlacement.class);
            profile.updateRoomPose(RoomPose.SLEEP_BED, bed, T1);

            profile.updateRoomPose(RoomPose.SLEEP_BED, bed, T2);

            assertThat(profile.getRoomPoseUpdatedAt()).isEqualTo(T1);
        }

        @Test
        @DisplayName("같은 포즈로 다른 가구에 옮겨가면 가구만 바뀌고 시각은 유지된다")
        void movingBetweenSameKindFurnitureKeepsTimestamp() {
            UserAvatarProfile profile = newProfile();
            UserRoomPlacement firstBed = mock(UserRoomPlacement.class);
            UserRoomPlacement secondBed = mock(UserRoomPlacement.class);
            profile.updateRoomPose(RoomPose.SLEEP_BED, firstBed, T1);

            profile.updateRoomPose(RoomPose.SLEEP_BED, secondBed, T2);

            assertThat(profile.getRoomPosePlacement()).isSameAs(secondBed);
            assertThat(profile.getRoomPoseUpdatedAt()).isEqualTo(T1);
        }
    }

    @Nested
    @DisplayName("가구가 방에서 빠지면 포즈를 무효화한다")
    class Cleared {

        // ddl-auto 가 ON DELETE SET NULL 을 안 만들어 주므로 서비스가 이걸 직접 불러야 한다.
        @Test
        @DisplayName("포즈·가구·시각을 모두 비운다")
        void clearsEverything() {
            UserAvatarProfile profile = newProfile();
            profile.updateRoomPose(RoomPose.SIT_DESK, mock(UserRoomPlacement.class), T1);

            profile.clearRoomPose();

            assertThat(profile.getRoomPose()).isNull();
            assertThat(profile.getRoomPosePlacement()).isNull();
            assertThat(profile.getRoomPoseUpdatedAt()).isNull();
        }

        @Test
        @DisplayName("비운 뒤 같은 포즈를 다시 취하면 시각이 새로 찍힌다")
        void reposingAfterClearStampsNewTime() {
            UserAvatarProfile profile = newProfile();
            profile.updateRoomPose(RoomPose.SIT_DESK, mock(UserRoomPlacement.class), T1);
            profile.clearRoomPose();

            profile.updateRoomPose(RoomPose.SIT_DESK, mock(UserRoomPlacement.class), T2);

            assertThat(profile.getRoomPoseUpdatedAt()).isEqualTo(T2);
        }
    }
}
