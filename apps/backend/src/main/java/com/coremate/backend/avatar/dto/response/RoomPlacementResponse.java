package com.coremate.backend.avatar.dto.response;

import tools.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "방에 배치된 가구")
public record RoomPlacementResponse(
        @Schema(example = "12") Long placementId,
        @Schema(example = "100") Long userItemId,
        @Schema(example = "3") Long itemId,
        @Schema(example = "기본 책상") String itemName,
        @Schema(example = "https://cdn.example.com/desk.png") String imageUrl,

        @Schema(description = "격자 x 좌표(0부터). 여러 칸 가구는 왼쪽 윗칸", example = "3") int gridX,
        @Schema(description = "격자 y 좌표(0부터)", example = "4") int gridY,

        @Schema(description = "렌더 부가 정보. footprint 로 점유 칸을, interaction 으로 포즈 유발 여부를 판단한다")
        JsonNode metadata
) {
}
