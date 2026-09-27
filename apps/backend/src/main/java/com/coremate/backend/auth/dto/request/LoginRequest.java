package com.coremate.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "로그인 요청")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @Schema(example = "user@example.com")
    @Email
    @NotBlank
    private String email;

    @Schema(example = "P@ssw0rd!")
    @NotBlank
    private String password;

    // 아래 기기 정보는 전부 선택 항목이다. 구버전 앱이 보내지 않아도 로그인은 그대로 되어야 하고,
    // 표시용 값 하나 때문에 로그인이 실패하면 안 되므로 @NotBlank 를 붙이지 않는다.

    @Schema(description = "앱이 보관 중인 기기 식별자. 없으면 서버가 발급해 응답으로 돌려준다.",
            example = "3f0c1b1e-2a4d-4f8b-9c1a-77d0f4e5a1b2")
    @Size(max = 64)
    private String deviceId;

    @Schema(description = "기기 목록에 표시할 이름", example = "iPhone 15 Pro")
    @Size(max = 64)
    private String deviceName;

    @Schema(description = "IOS / ANDROID / UNKNOWN. 알 수 없는 값은 UNKNOWN 으로 저장한다.",
            example = "IOS")
    @Size(max = 20)
    private String platform;

    @Schema(example = "18.2")
    @Size(max = 32)
    private String osVersion;
}
