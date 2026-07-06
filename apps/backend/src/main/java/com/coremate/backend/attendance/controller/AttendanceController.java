package com.coremate.backend.attendance.controller;

import com.coremate.backend.attendance.dto.response.AttendanceCalendarResponse;
import com.coremate.backend.attendance.dto.response.AttendanceResponse;
import com.coremate.backend.attendance.dto.response.AttendanceRewardResponse;
import com.coremate.backend.attendance.service.AttendanceService;
import com.coremate.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Attendance")
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @Operation(summary = "오늘 출석 조회")
    @GetMapping("/today")
    public AttendanceResponse getTodayAttendance(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return attendanceService.getTodayAttendance(userDetails.getUserId());
    }

    @Operation(summary = "출석 체크 상태 조회")
    @PostMapping("/checkin")
    public AttendanceResponse checkIn(@AuthenticationPrincipal CustomUserDetails userDetails) {
        // 출석 생성은 오늘의 퀴즈 제출에서만 수행하고, 이 API는 현재 출석 상태 확인용으로 둔다.
        return attendanceService.getTodayAttendance(userDetails.getUserId());
    }

    @Operation(summary = "출석 캘린더 조회")
    @GetMapping("/calendar")
    public AttendanceCalendarResponse getCalendar(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String month
    ) {
        return attendanceService.getCalendar(userDetails.getUserId(), month);
    }

    @Operation(summary = "출석 보상 수령")
    @PostMapping("/{attendanceId}/reward")
    public AttendanceRewardResponse claimReward(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long attendanceId
    ) {
        return attendanceService.claimReward(userDetails.getUserId(), attendanceId);
    }
}
