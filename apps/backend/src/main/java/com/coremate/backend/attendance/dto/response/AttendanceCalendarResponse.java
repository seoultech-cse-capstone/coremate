package com.coremate.backend.attendance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "출석 캘린더 응답")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceCalendarResponse {
    @Schema(example = "42")
    private Long userId;

    @Schema(example = "2026-05")
    private String month;

    @Schema(description = "해당 월 출석 일자")
    private List<LocalDate> attendedDates;

    @Schema(example = "12")
    private Integer totalCount;
}
