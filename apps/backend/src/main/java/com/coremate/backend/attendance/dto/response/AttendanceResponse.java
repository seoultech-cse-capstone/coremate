package com.coremate.backend.attendance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "출석 응답")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {
    @Schema(example = "1")
    private Long attendanceId;

    @Schema(example = "42")
    private Long userId;

    @Schema(example = "2026-05-12")
    private LocalDate attendanceDate;

    @Schema(example = "true")
    private Boolean rewardClaimed;

    @Schema(example = "2026-05-12T09:10:11")
    private LocalDateTime createdAt;
}
