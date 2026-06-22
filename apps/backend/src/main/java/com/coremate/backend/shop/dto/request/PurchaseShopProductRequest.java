package com.coremate.backend.shop.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "상점 상품 구매 요청")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseShopProductRequest {
    @Schema(example = "10")
    @NotNull
    private Long shopProductId;

    @Schema(example = "1")
    @Min(1)
    private Integer quantity;
}
