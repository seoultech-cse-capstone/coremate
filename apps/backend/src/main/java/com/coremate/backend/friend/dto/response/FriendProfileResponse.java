package com.coremate.backend.friend.dto.response;

import com.coremate.backend.avatar.dto.response.AvatarProfileResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "친구 프로필 응답")
public record FriendProfileResponse(
        @Schema(example = "99") Long userId,
        @Schema(example = "corefriend") String nickname,
        @Schema(example = "5") Integer level,
        @Schema(example = "서울과학기술대학교") String schoolName,
        @Schema(example = "true") Boolean schoolVerified,
        @Schema(example = "ACTIVE") String status,
        @Schema(example = "true") Boolean attendedToday,
        @Schema(example = "12") Long attendanceCount,
        @Schema(description = "친구의 공개 아바타 프로필") AvatarProfileResponse avatarProfile,
        @Schema(description = "친구 공개 과목별 요약 스탯") List<FriendSubjectStatResponse> subjectStats
) {
}
