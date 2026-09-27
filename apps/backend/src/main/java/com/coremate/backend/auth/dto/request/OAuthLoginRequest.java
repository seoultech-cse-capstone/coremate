package com.coremate.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "소셜 로그인 요청. provider SDK 가 앱에 준 토큰과 기기 정보를 넘긴다.")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthLoginRequest {

    @Schema(description = "provider가 앱에 발급한 access token", example = "provider-access-token")
    @NotBlank
    private String accessToken;

    @Schema(description = "google 로그인일 때만 사용하는 id token (선택)")
    private String idToken;

    // 아래 기기 정보는 LoginRequest 와 동일하게 전부 선택 항목이다.
    @Schema(description = "앱이 보관 중인 기기 식별자. 없으면 서버가 발급해 응답으로 돌려준다.",
            example = "3f0c1b1e-2a4d-4f8b-9c1a-77d0f4e5a1b2")
    @Size(max = 64)
    private String deviceId;

    @Schema(description = "기기 목록에 표시할 이름", example = "iPhone 15 Pro")
    @Size(max = 64)
    private String deviceName;

    @Schema(description = "IOS / ANDROID / UNKNOWN", example = "ANDROID")
    @Size(max = 20)
    private String platform;

    @Schema(example = "18.2")
    @Size(max = 32)
    private String osVersion;
}
