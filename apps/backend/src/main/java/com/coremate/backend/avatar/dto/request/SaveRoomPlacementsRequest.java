package com.coremate.backend.avatar.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "방 가구 배치 저장 요청. 사용자의 배치를 통째로 교체한다")
public record SaveRoomPlacementsRequest(
        @Valid
        @NotNull
        @Schema(description = "배치 전체. 빈 배열이면 방을 비운다")
        List<Placement> placements
) {
    @Schema(description = "가구 한 개의 자리")
    public record Placement(
            @NotNull
            @Schema(description = "배치할 보유 가구", example = "100") Long userItemId,

            @Schema(description = "격자 x 좌표(0부터). 여러 칸 가구는 왼쪽 윗칸", example = "3") int gridX,
            @Schema(description = "격자 y 좌표(0부터)", example = "4") int gridY
    ) {
    }
}
