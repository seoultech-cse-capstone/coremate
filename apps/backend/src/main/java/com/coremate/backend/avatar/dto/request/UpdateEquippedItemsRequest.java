package com.coremate.backend.avatar.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "장착 아이템 변경 요청")
public record UpdateEquippedItemsRequest(
        @Schema(description = "슬롯별 장착 아이템") List<EquippedItemSlot> equippedItems
) {
    @Schema(description = "장착 슬롯 매핑")
    public record EquippedItemSlot(
            @Schema(example = "HEAD") String slot,
            @Schema(example = "100") Long userItemId
    ) {
    }
}
