package com.coremate.backend.shop.dto.response;

import com.coremate.backend.shop.dto.type.ShopProductType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "상점 상품 응답")
public record ShopProductResponse(
        @Schema(example = "10") Long shopProductId,
        @Schema(example = "ITEM") ShopProductType productType,
        @Schema(example = "1") Long itemId,
        @Schema(example = "2") Long randomBoxId,
        @Schema(example = "Blue Hat") String productName,
        @Schema(example = "100") Integer priceCoin,
        @Schema(example = "1") Integer requiredLevel,
        @Schema(example = "true") Boolean isActive,
        @Schema(example = "2026-05-12T09:10:11") LocalDateTime createdAt
) {
}
