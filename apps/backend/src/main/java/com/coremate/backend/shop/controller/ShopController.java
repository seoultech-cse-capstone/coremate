package com.coremate.backend.shop.controller;

import com.coremate.backend.shop.dto.request.PurchaseShopProductRequest;
import com.coremate.backend.shop.dto.response.ShopProductResponse;
import com.coremate.backend.shop.dto.response.ShopPurchaseResponse;
import com.coremate.backend.shop.dto.type.ShopProductType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Shop")
@RestController
@RequestMapping("/api/shop")
public class ShopController {

    @Operation(summary = "상점 상품 조회")
    @GetMapping("/products")
    public List<ShopProductResponse> getProducts() {
        return List.of(new ShopProductResponse(
                10L,
                ShopProductType.ITEM,
                1L,
                null,
                "Blue Hat",
                100,
                1,
                true,
                LocalDateTime.now().minusDays(1)
        ));
    }

    @Operation(summary = "상점 상품 구매")
    @PostMapping("/purchase")
    public ShopPurchaseResponse purchase(@RequestBody PurchaseShopProductRequest request) {
        return new ShopPurchaseResponse(
                100L,
                42L,
                request.getShopProductId(),
                250,
                LocalDateTime.now()
        );
    }
}
