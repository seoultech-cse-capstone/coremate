package com.coremate.backend.avatar.inventory.controller;

import com.coremate.backend.global.security.CustomUserDetails;
import com.coremate.backend.global.config.OpenApiConfig;
import com.coremate.backend.avatar.inventory.dto.response.InventoryResponse;
import com.coremate.backend.avatar.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Inventory")
@RestController
@RequestMapping("/api/avatar/inventory")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "아이템 보관함 조회",
            description = "보유 아이템을 부위 13종으로 묶어 내려준다. 아이템이 없는 부위도 빈 목록으로"
                    + " 오므로 필터 칩을 바로 그릴 수 있다. 가구는 보유·배치·남은 개수를 함께 준다.")
    @GetMapping
    public InventoryResponse getInventory(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return inventoryService.getInventory(userDetails.getUserId());
    }
}
