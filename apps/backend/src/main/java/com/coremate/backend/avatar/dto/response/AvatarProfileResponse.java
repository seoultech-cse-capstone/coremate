package com.coremate.backend.avatar.dto.response;

import com.coremate.backend.avatar.dto.type.AvatarGenderType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "아바타 프로필 응답")
public record AvatarProfileResponse(
        @Schema(example = "42") Long userId,
        @Schema(example = "FEMALE") AvatarGenderType avatarGender,
        @Schema(example = "BASIC_01") String baseAvatarType,
        @Schema(description = "장착 아이템") List<AvatarEquippedItemResponse> equippedItems,
        @Schema(example = "2026-05-12T09:10:11") LocalDateTime updatedAt
) {
}
