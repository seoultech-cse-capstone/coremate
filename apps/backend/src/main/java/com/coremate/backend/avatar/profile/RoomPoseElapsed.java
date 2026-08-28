package com.coremate.backend.avatar.profile;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 친구 방에 붙는 "3일 전부터 자는 중" 같은 경과 시간 라벨을 만든다.
 *
 * <p>⚠️ 포즈는 접속 상태가 아니다. 3일 전에 로그아웃한 친구도 자는 것으로 보이므로
 * 경과 시간을 반드시 함께 보여 "지금 접속 중"으로 오해하지 않게 한다(SCR-FRIEND-02).
 * 라벨을 서버가 만드는 이유가 이것이다 - 클라이언트가 빠뜨리면 오해가 그대로 남는다.
 */
public final class RoomPoseElapsed {

    private RoomPoseElapsed() {
    }

    /**
     * STAND 이거나 포즈를 한 번도 바꾼 적이 없으면 표시하지 않는다(null).
     */
    public static String label(RoomPose pose, LocalDateTime poseUpdatedAt, LocalDateTime now) {
        if (pose == null || pose == RoomPose.STAND || poseUpdatedAt == null) {
            return null;
        }
        Duration elapsed = Duration.between(poseUpdatedAt, now);
        if (elapsed.isNegative()) {
            // 시계 오차 등으로 미래 시각이 들어오면 방금으로 본다.
            return "방금 전부터";
        }
        long hours = elapsed.toHours();
        if (hours < 1) {
            return "방금 전부터";
        }
        if (hours < 24) {
            return hours + "시간 전부터";
        }
        return elapsed.toDays() + "일 전부터";
    }
}
