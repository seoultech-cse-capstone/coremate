package com.coremate.backend.inventory.controller;

import com.coremate.backend.inventory.dto.response.UserItemInventoryResponse;
import com.coremate.backend.inventory.dto.response.UserItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Inventory")
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Operation(summary = "내 인벤토리 조회")
    @GetMapping
    public UserItemInventoryResponse getInventory() {
        return new UserItemInventoryResponse(42L, sampleItems());
    }

    @Operation(summary = "내 아이템 목록 조회")
    @GetMapping("/items")
    public List<UserItemResponse> getItems() {
        return sampleItems();
    }

    private List<UserItemResponse> sampleItems() {
        return List.of(new UserItemResponse(
                100L,
                42L,
                1L,
                "Blue Hat",
                "HEAD",
                "HEAD",
                "RARE",
                "https://cdn.example.com/hat.png",
                1,
                "SHOP",
                LocalDateTime.now().minusDays(1)
        ));
    }
}
