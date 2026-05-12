package com.coremate.backend.shop.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "랜덤박스 열기 요청")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenRandomBoxRequest {
    @Schema(example = "3")
    @NotNull
    private Long randomBoxId;
}
