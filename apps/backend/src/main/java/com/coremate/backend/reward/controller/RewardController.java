package com.coremate.backend.reward.controller;

import com.coremate.backend.reward.dto.response.DailyRewardLimitResponse;
import com.coremate.backend.reward.dto.response.RewardLogResponse;
import com.coremate.backend.reward.dto.response.UserRewardSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reward")
@RestController
@RequestMapping("/api/rewards")
public class RewardController {

    @Operation(summary = "보상 요약 조회")
    @GetMapping("/summary")
    public UserRewardSummaryResponse getSummary() {
        return new UserRewardSummaryResponse(42L, 1200, 5, 350);
    }

    @Operation(summary = "보상 로그 조회")
    @GetMapping("/logs")
    public List<RewardLogResponse> getLogs() {
        return List.of(new RewardLogResponse(
                1L,
                42L,
                "DAILY_ATTENDANCE",
                10L,
                5,
                50,
                null,
                null,
                LocalDateTime.now().minusDays(1)
        ));
    }

    @Operation(summary = "일일 보상 제한 조회")
    @GetMapping("/daily-limits")
    public List<DailyRewardLimitResponse> getDailyLimits() {
        return List.of(new DailyRewardLimitResponse(
                1L,
                42L,
                LocalDate.now(),
                "DAILY_ATTENDANCE",
                1,
                LocalDateTime.now()
        ));
    }
}
