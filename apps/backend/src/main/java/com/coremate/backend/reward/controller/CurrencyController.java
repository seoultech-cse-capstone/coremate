package com.coremate.backend.reward.controller;

import com.coremate.backend.reward.dto.response.CurrencyLogResponse;
import com.coremate.backend.reward.dto.response.UserCurrencyResponse;
import com.coremate.backend.reward.dto.type.CurrencyType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Currency")
@RestController
@RequestMapping("/api/currency")
public class CurrencyController {

    @Operation(summary = "내 재화 조회")
    @GetMapping("/me")
    public UserCurrencyResponse getMyCurrency() {
        return new UserCurrencyResponse(42L, 350, LocalDateTime.now());
    }

    @Operation(summary = "재화 로그 조회")
    @GetMapping("/logs")
    public List<CurrencyLogResponse> getLogs() {
        return List.of(new CurrencyLogResponse(
                1L,
                42L,
                -100,
                250,
                CurrencyType.COIN,
                "SHOP_PURCHASE",
                "SHOP",
                10L,
                LocalDateTime.now().minusDays(1)
        ));
    }
}
