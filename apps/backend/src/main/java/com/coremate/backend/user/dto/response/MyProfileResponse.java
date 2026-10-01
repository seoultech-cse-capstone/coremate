package com.coremate.backend.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "내 프로필 응답")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyProfileResponse {
    @Schema(example = "42")
    private Long userId;

    @Schema(example = "user@example.com")
    private String email;

    @Schema(example = "coremate")
    private String nickname;

    @Schema(example = "USER")
    private String role;

    @Schema(example = "1")
    private Integer level;

    @Schema(example = "1200")
    private Integer totalXp;

    @Schema(example = "School A")
    private String schoolName;

    @Schema(example = "student@school.edu")
    private String schoolEmail;

    @Schema(example = "false")
    private Boolean schoolVerified;

    @Schema(example = "ACTIVE")
    private String status;

    @Schema(example = "2026-05-12T09:10:11")
    private LocalDateTime createdAt;

    @Schema(example = "2026-05-12T09:10:11")
    private LocalDateTime lastLoginAt;

    @Schema(example = "true", description = "오늘 출석(오늘의 퀴즈 완료) 여부")
    private Boolean attendedToday;

    @Schema(example = "12", description = "누적 출석 일수")
    private Long attendanceCount;

    @Schema(example = "2026-05-12", description = "가장 최근 출석일 (없으면 null)")
    private LocalDate lastAttendanceDate;

    @Schema(example = "true", description = "친구에게 학습 기록(과목별 스탯·Learning History) 공개 여부")
    private Boolean learningPublic;
}
