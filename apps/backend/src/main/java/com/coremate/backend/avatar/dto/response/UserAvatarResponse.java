package com.coremate.backend.avatar.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "사용자 아바타 응답")
public record UserAvatarResponse(
        @Schema(example = "42") Long userId,
        @Schema(description = "프로필 정보") AvatarProfileResponse profile,
        @Schema(description = "보유 아바타 아이템") List<AvatarItemResponse> items
) {
}
