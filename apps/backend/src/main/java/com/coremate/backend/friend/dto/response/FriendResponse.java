package com.coremate.backend.friend.dto.response;

import com.coremate.backend.avatar.dto.response.AvatarProfileResponse;
import com.coremate.backend.friend.dto.type.FriendStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "친구 목록 응답")
public record FriendResponse(
        @Schema(example = "123") Long friendId,
        @Schema(example = "99") Long friendUserId,
        @Schema(example = "corefriend") String nickname,
        @Schema(example = "서울과학기술대학교") String schoolName,
        @Schema(example = "5") Integer level,
        @Schema(example = "ACCEPTED") FriendStatus status,
        @Schema(example = "true") Boolean attendedToday,
        @Schema(example = "12") Long attendanceCount,
        @Schema(description = "친구의 공개 아바타 프로필") AvatarProfileResponse avatarProfile,
        @Schema(example = "2026-05-12T09:10:11") LocalDateTime createdAt,
        @Schema(example = "2026-05-12T09:10:11") LocalDateTime acceptedAt
) {
}
