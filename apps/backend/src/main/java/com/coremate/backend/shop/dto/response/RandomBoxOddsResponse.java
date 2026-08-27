package com.coremate.backend.shop.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

// 랜덤박스의 등급별 노출 확률. random_box_items 행을 등급 버킷으로 합산한 값이라
// 화면 표시 확률의 합이 DB 저장 확률의 합과 정확히 일치한다. (FR-SHOP-03)
@Schema(description = "랜덤박스 등급별 확률 응답")
public record RandomBoxOddsResponse(
        @Schema(example = "COMMON", description = "아이템 등급(COMMON/RARE/EPIC) 또는 코인 보상(COIN)") String grade,
        @Schema(example = "0.450000") String probability
) {
}
