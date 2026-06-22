package com.coremate.backend.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "로그인 응답")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    @Schema(example = "access-token")
    private String accessToken;

    @Schema(example = "refresh-token")
    private String refreshToken;

    @Schema(example = "3600")
    private Long expiresInSec;

    @Schema(example = "42")
    private Long userId;

    @Schema(example = "coremate")
    private String nickname;
}
