package com.coremate.backend.avatar.dto.response;

import com.coremate.backend.avatar.dto.type.AvatarItemType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "아바타 아이템 응답")
public record AvatarItemResponse(
        @Schema(example = "1") Long itemId,
        @Schema(example = "Blue Hat") String itemName,
        @Schema(example = "HEAD") AvatarItemType itemType,
        @Schema(example = "HEAD") String slot,
        @Schema(example = "RARE") String rarity,
        @Schema(example = "https://cdn.example.com/hat.png") String imageUrl,
        @Schema(example = "A cool blue hat") String description,
        @Schema(example = "true") Boolean isActive
) {
}
