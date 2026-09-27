package com.coremate.backend.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "비밀번호 재설정 토큰 응답")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetTokenResponse {

    @Schema(description = "비밀번호 재설정에 사용할 단기 토큰", example = "e2b1c0d4-...")
    private String resetToken;

    @Schema(description = "재설정 토큰 유효시간(초)", example = "600")
    private long expiresInSec;
}
