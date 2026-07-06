package com.coremate.backend.reward.controller;

import com.coremate.backend.global.security.CustomUserDetails;
import com.coremate.backend.reward.dto.response.UserRewardSummaryResponse;
import com.coremate.backend.reward.dto.response.XpLogResponse;
import com.coremate.backend.reward.service.RewardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "XP")
@RestController
@RequestMapping("/api/xp")
@RequiredArgsConstructor
public class XpController {

    private final RewardService rewardService;

    @Operation(summary = "경험치 요약 조회")
    @GetMapping("/summary")
    public UserRewardSummaryResponse getSummary(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return rewardService.getSummary(userDetails.getUserId());
    }

    @Operation(summary = "경험치 로그 조회")
    @GetMapping("/logs")
    public List<XpLogResponse> getLogs(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return rewardService.getXpLogs(userDetails.getUserId());
    }
}
