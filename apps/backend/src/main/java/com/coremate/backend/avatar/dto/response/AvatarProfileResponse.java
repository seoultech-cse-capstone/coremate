package com.coremate.backend.avatar.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "아바타 프로필 응답")
public record AvatarProfileResponse(
        @Schema(example = "42") Long userId,
        @Schema(example = "BASIC_01", description = "베이스 마네킹. 성별 구분 없이 BASIC_01 고정") String baseAvatarType,
        @Schema(description = "장착 아이템") List<AvatarEquippedItemResponse> equippedItems,
        @Schema(example = "2026-05-12T09:10:11") LocalDateTime updatedAt
) {
}
