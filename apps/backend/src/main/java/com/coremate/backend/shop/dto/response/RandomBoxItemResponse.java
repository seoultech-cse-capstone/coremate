package com.coremate.backend.shop.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "랜덤박스 아이템 응답")
public record RandomBoxItemResponse(
        @Schema(example = "1") Long itemId,
        @Schema(example = "Blue Hat") String itemName,
        @Schema(example = "ITEM") String rewardType,
        @Schema(example = "50") Integer coinAmount,
        @Schema(example = "0.250000") String probability
) {
}
