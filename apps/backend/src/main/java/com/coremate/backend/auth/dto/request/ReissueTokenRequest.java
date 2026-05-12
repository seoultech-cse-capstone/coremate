package com.coremate.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "토큰 재발급 요청")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReissueTokenRequest {
    @Schema(example = "refresh-token")
    @NotBlank
    private String refreshToken;
}
