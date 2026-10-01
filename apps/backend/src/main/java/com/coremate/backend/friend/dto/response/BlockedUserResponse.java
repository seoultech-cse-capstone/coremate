package com.coremate.backend.friend.dto.response;

import com.coremate.backend.avatar.dto.response.AvatarEquippedItemResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "내가 차단한 사용자")
public record BlockedUserResponse(
        @Schema(example = "99") Long userId,
        @Schema(example = "corefriend") String nickname,
        @Schema(example = "2026-10-01T15:30:00") LocalDateTime blockedAt,
        @Schema(description = "지금 장착한 아이템(렌더 순서). 목록 아이콘용이라 장착이 없으면 빈 배열")
        List<AvatarEquippedItemResponse> equippedItems
) {
}
