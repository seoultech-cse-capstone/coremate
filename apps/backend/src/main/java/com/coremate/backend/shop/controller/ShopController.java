package com.coremate.backend.shop.controller;

import com.coremate.backend.global.config.OpenApiConfig;
import com.coremate.backend.global.security.CustomUserDetails;
import com.coremate.backend.shop.dto.request.PurchaseShopProductRequest;
import com.coremate.backend.shop.dto.response.ShopProductResponse;
import com.coremate.backend.shop.dto.response.ShopPurchaseResponse;
import com.coremate.backend.shop.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Shop")
@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class ShopController {

    private final ShopService shopService;

    @Operation(summary = "상점 상품 조회")
    @GetMapping("/products")
    public List<ShopProductResponse> getProducts() {
        return shopService.getProducts();
    }

    @Operation(summary = "상점 상품 구매")
    @PostMapping("/purchase")
    public ShopPurchaseResponse purchase(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PurchaseShopProductRequest request
    ) {
        return shopService.purchase(userDetails.getUserId(), request);
    }
}
