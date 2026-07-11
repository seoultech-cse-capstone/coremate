package com.coremate.backend.friend.dto.response;

import com.coremate.backend.friend.dto.type.FriendStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "친구 검색 사용자 응답")
public record FriendSearchUserResponse(
        @Schema(example = "99") Long userId,
        @Schema(example = "corefriend") String nickname,
        @Schema(example = "서울과학기술대학교") String schoolName,
        @Schema(example = "5") Integer level,
        @Schema(example = "REQUESTED") FriendStatus relationshipStatus
) {
}
