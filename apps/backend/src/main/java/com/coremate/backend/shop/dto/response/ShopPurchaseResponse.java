package com.coremate.backend.shop.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "상점 구매 응답")
public record ShopPurchaseResponse(
        @Schema(example = "100", description = "상점 구매 로그 ID") Long shopPurchaseId,
        @Schema(example = "42", description = "구매 사용자 ID") Long userId,
        @Schema(example = "10", description = "구매한 상품 ID") Long shopProductId,
        @Schema(example = "250", description = "구매 후 남은 코인 잔액") Integer remainingCoin,
        @Schema(example = "2026-05-12T09:10:11", description = "구매 시각") LocalDateTime purchasedAt
) {
}
