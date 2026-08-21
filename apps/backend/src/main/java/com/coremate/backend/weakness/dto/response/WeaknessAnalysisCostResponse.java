package com.coremate.backend.weakness.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 분석표 생성 비용 사전 조회 응답(SCR-WEAK-03 "필요한 코인").
 *
 * <p>생성 요청과 같은 정책 조회를 거치므로 여기서 안내한 값과 실제 차감액이 어긋나지 않는다.
 */
@Schema(description = "약점 분석표 생성 비용 응답 DTO")
public record WeaknessAnalysisCostResponse(
        @Schema(description = "분석표 1건 생성에 필요한 코인", example = "100")
        int coinCost
) {
}
