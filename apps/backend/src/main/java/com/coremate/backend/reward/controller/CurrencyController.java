package com.coremate.backend.reward.controller;

import com.coremate.backend.global.security.CustomUserDetails;
import com.coremate.backend.reward.dto.response.CurrencyLogResponse;
import com.coremate.backend.reward.dto.response.UserCurrencyResponse;
import com.coremate.backend.reward.service.RewardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Currency")
@RestController
@RequestMapping("/api/currency")
@RequiredArgsConstructor
public class CurrencyController {

    private final RewardService rewardService;

    @Operation(summary = "내 재화 조회")
    @GetMapping("/me")
    public UserCurrencyResponse getMyCurrency(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return rewardService.getMyCurrency(userDetails.getUserId());
    }

    @Operation(summary = "재화 로그 조회")
    @GetMapping("/logs")
    public List<CurrencyLogResponse> getLogs(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return rewardService.getCurrencyLogs(userDetails.getUserId());
    }
}
