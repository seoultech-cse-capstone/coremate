package com.coremate.backend.attendance.controller;

import com.coremate.backend.attendance.dto.response.AttendanceCalendarResponse;
import com.coremate.backend.attendance.dto.response.AttendanceResponse;
import com.coremate.backend.attendance.dto.response.AttendanceRewardResponse;
import com.coremate.backend.global.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Attendance")
@RestController
@RequestMapping("/api/attendance")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class AttendanceController {

    @Operation(summary = "출석 체크")
    @PostMapping("/checkin")
    public AttendanceResponse checkIn() {
        return AttendanceResponse.builder()
                .attendanceId(1L)
                .userId(42L)
                .attendanceDate(LocalDate.now())
                .rewardClaimed(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Operation(summary = "출석 캘린더 조회")
    @GetMapping("/calendar")
    public AttendanceCalendarResponse getCalendar() {
        return AttendanceCalendarResponse.builder()
                .userId(42L)
                .month("2026-05")
                .attendedDates(List.of(LocalDate.now().minusDays(2), LocalDate.now()))
                .totalCount(2)
                .build();
    }

    @Operation(summary = "출석 보상 수령")
    @PostMapping("/{attendanceId}/reward")
    public AttendanceRewardResponse claimReward(@PathVariable Long attendanceId) {
        return AttendanceRewardResponse.builder()
                .attendanceId(attendanceId)
                .coinAmount(50)
                .xpAmount(10)
                .rewardClaimed(true)
                .rewardDate(LocalDate.now())
                .build();
    }
}
