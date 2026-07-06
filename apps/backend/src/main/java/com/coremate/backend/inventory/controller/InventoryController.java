package com.coremate.backend.inventory.controller;

import com.coremate.backend.global.security.CustomUserDetails;
import com.coremate.backend.inventory.dto.response.UserItemInventoryResponse;
import com.coremate.backend.inventory.dto.response.UserItemResponse;
import com.coremate.backend.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Inventory")
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "내 인벤토리 조회")
    @GetMapping
    public UserItemInventoryResponse getInventory(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return inventoryService.getInventory(userDetails.getUserId());
    }

    @Operation(summary = "내 아이템 목록 조회")
    @GetMapping("/items")
    public List<UserItemResponse> getItems(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return inventoryService.getItems(userDetails.getUserId());
    }
}
