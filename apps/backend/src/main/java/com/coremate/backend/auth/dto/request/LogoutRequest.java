package com.coremate.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "로그아웃 요청")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequest {
    @Schema(example = "refresh-token")
    @NotBlank
    private String refreshToken;
}
