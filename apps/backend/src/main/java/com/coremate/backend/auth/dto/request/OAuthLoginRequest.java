package com.coremate.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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

    // apple 은 앱에 access token 을 주지 않아 provider 마다 필수 여부가 달라서, 비었는지는 각 OAuthClient 가 본다.
    @Schema(description = "provider가 앱에 발급한 access token. kakao·naver·google 은 필수, apple 은 보내지 않는다.",
            example = "provider-access-token")
    private String accessToken;

    @Schema(description = "google·apple 로그인에서 쓰는 id token. apple 은 필수(identityToken).")
    private String idToken;

    @Schema(description = "apple 로그인일 때만 쓰는 authorization code. 탈퇴할 때 Apple 연결을 끊는 데 필요하다.")
    private String authorizationCode;

    @Schema(description = "apple 로그인일 때만 쓰는 이름. Apple 은 처음 로그인할 때만 앱에 이름을 준다.", example = "홍길동")
    @Size(max = 50)
    private String name;

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
