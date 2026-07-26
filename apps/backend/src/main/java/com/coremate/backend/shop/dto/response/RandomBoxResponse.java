package com.coremate.backend.shop.dto.response;

import com.coremate.backend.shop.dto.type.RandomBoxGrade;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "랜덤박스 응답")
public record RandomBoxResponse(
        @Schema(example = "3") Long randomBoxId,
        @Schema(example = "Starter Box") String boxName,
        @Schema(example = "COMMON") RandomBoxGrade boxGrade,
        @Schema(example = "1") Integer requiredLevel,
        @Schema(example = "100") Integer priceCoin,
        @Schema(example = "true") Boolean isActive
) {
}
