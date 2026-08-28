package com.coremate.backend.avatar.dto.response;

import com.coremate.backend.avatar.profile.RoomPose;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "내 방 조회 응답")
public record RoomResponse(
        @Schema(example = "42") Long userId,
        @Schema(example = "BASIC_01") String baseAvatarType,

        @Schema(description = "방 격자 가로 칸 수", example = "10") int gridWidth,
        @Schema(description = "방 격자 세로 칸 수", example = "7") int gridHeight,

        @Schema(description = "장착 아이템. renderOrder 순으로 정렬되어 있다")
        List<AvatarEquippedItemResponse> equippedItems,

        @Schema(description = "배치된 가구") List<RoomPlacementResponse> placements,

        @Schema(description = "마지막에 취한 포즈. null 은 STAND 와 같다", example = "SLEEP_BED")
        RoomPose roomPose,

        @Schema(description = "그 포즈를 취한 가구. 배치가 유효하지 않으면 null 로 내려간다", example = "12")
        Long roomPosePlacementId,

        @Schema(description = "포즈가 마지막으로 바뀐 시각. 친구 방의 경과 시간 표시 기준",
                example = "2026-08-25T10:00:00")
        LocalDateTime roomPoseUpdatedAt
) {
}
