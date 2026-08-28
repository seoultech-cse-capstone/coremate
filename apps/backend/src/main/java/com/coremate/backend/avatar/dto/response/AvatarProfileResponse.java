package com.coremate.backend.avatar.dto.response;

import com.coremate.backend.avatar.profile.RoomPose;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "아바타 프로필 응답")
public record AvatarProfileResponse(
        @Schema(example = "42") Long userId,
        @Schema(example = "BASIC_01", description = "베이스 마네킹. 성별 구분 없이 BASIC_01 고정") String baseAvatarType,
        @Schema(description = "장착 아이템") List<AvatarEquippedItemResponse> equippedItems,

        @Schema(description = "마지막에 취한 포즈. null 은 STAND 와 같다", example = "SLEEP_BED")
        RoomPose roomPose,

        @Schema(description = "포즈가 마지막으로 바뀐 시각", example = "2026-08-25T10:00:00")
        LocalDateTime roomPoseUpdatedAt,

        // ⚠️ 접속 상태가 아니다. 이 라벨 없이 포즈만 보여주면 "지금 자고 있다"로 오해한다.
        @Schema(description = "친구 방에 붙이는 경과 시간 라벨. STAND 이거나 시각이 없으면 null",
                example = "3일 전부터") String roomPoseElapsedText,

        @Schema(example = "2026-05-12T09:10:11") LocalDateTime updatedAt
) {
}
