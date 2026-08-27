package com.coremate.backend.avatar.dto.response;

import com.coremate.backend.avatar.catalog.type.AvatarItemType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "아바타 장착 아이템 응답")
public record AvatarEquippedItemResponse(
        @Schema(example = "HAIR") String slot,
        @Schema(example = "100") Long userItemId,
        @Schema(example = "1") Long itemId,
        @Schema(example = "Blue Hat") String itemName,
        @Schema(example = "HAIR") AvatarItemType itemType,
        @Schema(example = "RARE") String rarity,
        @Schema(example = "https://cdn.example.com/hat.png") String imageUrl,

        // 정면 기준 겹쳐 그리는 순서. 아바타 스프라이트 스택이 아니면 null.
        @Schema(example = "5") Integer renderOrder
) {
}
