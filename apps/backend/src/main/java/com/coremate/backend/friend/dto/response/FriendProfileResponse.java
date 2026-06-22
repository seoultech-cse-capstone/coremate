package com.coremate.backend.friend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Friend profile response")
public record FriendProfileResponse(
        @Schema(example = "99") Long userId,
        @Schema(example = "friend") String nickname,
        @Schema(example = "5") Integer level,
        @Schema(example = "ACTIVE") String status,
        @Schema(example = "https://cdn.example.com/avatar.png") String avatarImageUrl
) {
}
