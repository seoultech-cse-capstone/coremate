package com.coremate.backend.avatar.inventory.dto.response;

import com.coremate.backend.avatar.catalog.type.AvatarItemType;
import com.coremate.backend.avatar.catalog.type.AvatarSlot;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "부위별 묶음. 13종이 항상 모두 내려가므로 비어 있어도 필터 칩을 그릴 수 있다")
public record InventoryGroupResponse(
        @Schema(example = "HAIR") AvatarItemType itemType,
        @Schema(description = "이 부위의 장착 슬롯. FURNITURE 는 슬롯이 없어 null", example = "HAIR")
        AvatarSlot slot,
        @Schema(example = "2") int count,
        List<InventoryItemResponse> items
) {
}
