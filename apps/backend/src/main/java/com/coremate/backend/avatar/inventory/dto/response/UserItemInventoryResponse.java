package com.coremate.backend.avatar.inventory.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "사용자 인벤토리 응답")
public record UserItemInventoryResponse(
        @Schema(example = "42") Long userId,
        @Schema(description = "사용자가 보유한 아이템 목록") List<UserItemResponse> items
) {
}
