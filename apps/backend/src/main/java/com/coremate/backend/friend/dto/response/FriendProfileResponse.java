package com.coremate.backend.friend.dto.response;

import com.coremate.backend.avatar.dto.response.AvatarProfileResponse;
import com.coremate.backend.avatar.dto.response.RoomResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "친구 프로필 응답")
public record FriendProfileResponse(
        @Schema(example = "99") Long userId,
        @Schema(example = "corefriend") String nickname,
        @Schema(example = "5") Integer level,
        @Schema(example = "서울과학기술대학교") String schoolName,
        @Schema(example = "true") Boolean schoolVerified,
        @Schema(example = "ACTIVE") String status,
        @Schema(example = "true", description = "오늘 출석 여부") Boolean attendedToday,
        @Schema(example = "12", description = "누적 출석 일수") Long attendanceCount,
        @Schema(example = "2026-05-12", description = "가장 최근 출석일 (없으면 null)") LocalDate lastAttendanceDate,
        @Schema(description = "친구의 공개 아바타 프로필") AvatarProfileResponse avatarProfile,

        // 읽기 전용이다. 친구는 배치를 바꿀 수 없고 조회만 된다(FR-AVATAR-04).
        @Schema(description = "친구의 방. 벽지·바닥·배치 가구와 마지막 포즈까지 그대로 보여준다")
        RoomResponse room,
        @Schema(description = "친구 공개 과목별 요약 스탯") List<FriendSubjectStatResponse> subjectStats
) {
}
