package com.coremate.backend.reward.controller;

import com.coremate.backend.reward.dto.response.UserRewardSummaryResponse;
import com.coremate.backend.reward.dto.response.XpLogResponse;
import com.coremate.backend.reward.dto.type.XpReasonType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "XP")
@RestController
@RequestMapping("/api/xp")
public class XpController {

    @Operation(summary = "경험치 요약 조회")
    @GetMapping("/summary")
    public UserRewardSummaryResponse getSummary() {
        return new UserRewardSummaryResponse(42L, 1200, 5, 350);
    }

    @Operation(summary = "경험치 로그 조회")
    @GetMapping("/logs")
    public List<XpLogResponse> getLogs() {
        return List.of(new XpLogResponse(
                1L,
                42L,
                10,
                1200,
                XpReasonType.SOLVE_QUESTION,
                "QUESTION",
                101L,
                LocalDateTime.now().minusHours(2)
        ));
    }
}
