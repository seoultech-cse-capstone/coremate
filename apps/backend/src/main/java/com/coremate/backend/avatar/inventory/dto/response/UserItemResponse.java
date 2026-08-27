package com.coremate.backend.avatar.inventory.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "사용자 아이템 응답")
public record UserItemResponse(
        @Schema(example = "100") Long userItemId,
        @Schema(example = "42") Long userId,
        @Schema(example = "1") Long itemId,
        @Schema(example = "Blue Hat") String itemName,
        @Schema(example = "HEAD") String itemType,
        @Schema(example = "HEAD") String slot,
        @Schema(example = "RARE") String rarity,
        @Schema(example = "https://cdn.example.com/hat.png") String imageUrl,
        @Schema(example = "1") Integer quantity,
        @Schema(example = "SHOP") String acquiredSource,
        @Schema(example = "2026-05-12T09:10:11") LocalDateTime acquiredAt
) {
}
