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

    @Schema(description = "이 기기에 저장해 두고 다음 로그인에 그대로 보낼 기기 식별자",
            example = "3f0c1b1e-2a4d-4f8b-9c1a-77d0f4e5a1b2")
    private String deviceId;
}
