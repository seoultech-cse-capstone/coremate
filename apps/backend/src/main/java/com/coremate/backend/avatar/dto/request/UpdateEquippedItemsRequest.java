package com.coremate.backend.avatar.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "장착 아이템 변경 요청")
public record UpdateEquippedItemsRequest(
        @Valid
        @NotEmpty
        @Schema(description = "슬롯별 장착 아이템") List<EquippedItemSlot> equippedItems
) {
    @Schema(description = "장착 슬롯 매핑")
    public record EquippedItemSlot(
            @NotBlank
            @Schema(example = "HAIR") String slot,

            @Schema(description = "null이면 해당 슬롯 장착 해제", example = "100")
            Long userItemId
    ) {
    }
}
