package com.coremate.backend.attendance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "출석 보상 응답")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRewardResponse {
    @Schema(example = "1")
    private Long attendanceId;

    @Schema(example = "50")
    private Integer coinAmount;

    @Schema(example = "10")
    private Integer xpAmount;

    @Schema(example = "true")
    private Boolean rewardClaimed;

    @Schema(example = "2026-05-12")
    private LocalDate rewardDate;
}
