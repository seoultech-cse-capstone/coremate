package com.coremate.backend.avatar.inventory.dto.response;

import com.coremate.backend.avatar.catalog.type.AvatarItemType;
import com.coremate.backend.avatar.catalog.type.AvatarSlot;
import com.coremate.backend.avatar.catalog.type.ItemRarity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import tools.jackson.databind.JsonNode;

@Schema(description = "보관함의 아이템 한 개")
public record InventoryItemResponse(
        @Schema(example = "100") Long userItemId,
        @Schema(example = "1") Long itemId,
        @Schema(example = "기본 머리") String itemName,
        @Schema(example = "HAIR") AvatarItemType itemType,
        @Schema(description = "장착 슬롯. FURNITURE 는 null", example = "HAIR") AvatarSlot slot,
        @Schema(example = "COMMON") ItemRarity rarity,
        @Schema(example = "https://cdn.example.com/hair.png") String imageUrl,

        @Schema(description = "기본 지급 비매품 여부. 상점에도 랜덤박스에도 나오지 않는다", example = "true")
        boolean defaultItem,

        @Schema(description = "지금 장착 중인지. 가구는 항상 false 이며 배치 개수로 판단한다", example = "true")
        boolean equipped,

        @Schema(description = "보유 수량", example = "3") int quantity,

        @Schema(description = "방에 놓인 개수. 가구만 값이 있고 나머지는 null", example = "1")
        Integer placedCount,

        @Schema(description = "아직 놓을 수 있는 개수(보유 - 배치). 가구만 값이 있다", example = "2")
        Integer remainingCount,

        @Schema(description = "렌더 부가 정보. 가구는 footprint 로 점유 칸을 판단한다")
        JsonNode metadata,

        @Schema(example = "2026-08-25T09:10:11") LocalDateTime acquiredAt
) {
}
