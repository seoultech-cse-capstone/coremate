package com.coremate.backend.avatar.inventory.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "아이템 보관함 응답")
public record InventoryResponse(
        @Schema(example = "42") Long userId,
        @Schema(example = "9") int totalCount,
        @Schema(description = "부위별 묶음 13종. 아이템이 없는 부위도 빈 목록으로 내려간다")
        List<InventoryGroupResponse> groups
) {
}
