package com.coremate.backend.reward.controller;

import com.coremate.backend.reward.service.AdRewardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reward")
@RestController
@RequestMapping("/api/rewards/ads")
@RequiredArgsConstructor
public class AdRewardController {

    private final AdRewardService adRewardService;

    // 구글(AdMob)이 부르는 주소라 토큰 없이 열려 있고(SecurityConfig), 서명으로 출처를 확인한다. 앱은 부르지 않는다.
    // 서명 검증에는 받은 그대로의 쿼리 문자열이 필요해서 파라미터와 따로 넘긴다.
    @Operation(
            summary = "AdMob 보상형 광고 시청 완료 콜백(서버 측 확인)",
            description = "광고 시청이 끝나면 구글이 호출한다. 서명이 맞으면 지급 여부와 관계없이 200 을 반환한다."
    )
    @GetMapping("/admob-ssv")
    public ResponseEntity<Void> handleAdMobCallback(
            HttpServletRequest request,
            @RequestParam(name = "ad_unit", required = false) String adUnit,
            @RequestParam(name = "transaction_id", required = false) String transactionId,
            @RequestParam(name = "user_id", required = false) String userId
    ) {
        adRewardService.handleAdMobCallback(request.getQueryString(), adUnit, transactionId, userId);
        return ResponseEntity.ok().build();
    }
}
