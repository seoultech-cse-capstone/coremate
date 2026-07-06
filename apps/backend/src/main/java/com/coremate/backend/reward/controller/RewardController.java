package com.coremate.backend.reward.controller;

import com.coremate.backend.global.security.CustomUserDetails;
import com.coremate.backend.reward.dto.response.DailyRewardLimitResponse;
import com.coremate.backend.reward.dto.response.RewardLogResponse;
import com.coremate.backend.reward.dto.response.RewardPolicyResponse;
import com.coremate.backend.reward.dto.response.UserRewardSummaryResponse;
import com.coremate.backend.reward.service.RewardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reward")
@RestController
@RequestMapping("/api/rewards")
@RequiredArgsConstructor
public class RewardController {

    private final RewardService rewardService;

    @Operation(summary = "보상 요약 조회")
    @GetMapping("/summary")
    public UserRewardSummaryResponse getSummary(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return rewardService.getSummary(userDetails.getUserId());
    }

    @Operation(summary = "보상 로그 조회")
    @GetMapping("/logs")
    public List<RewardLogResponse> getLogs(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return rewardService.getRewardLogs(userDetails.getUserId());
    }

    @Operation(summary = "일일 보상 제한 조회")
    @GetMapping("/daily-limits")
    public List<DailyRewardLimitResponse> getDailyLimits(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate rewardDate
    ) {
        return rewardService.getDailyLimits(userDetails.getUserId(), rewardDate);
    }

    @Operation(summary = "활성 보상 정책 조회")
    @GetMapping("/policies")
    public List<RewardPolicyResponse> getActivePolicies() {
        return rewardService.getActivePolicies();
    }
}
