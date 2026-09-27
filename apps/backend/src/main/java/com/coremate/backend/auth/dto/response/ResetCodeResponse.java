package com.coremate.backend.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "비밀번호 재설정 인증번호 발송 응답")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetCodeResponse {

    @Schema(description = "인증번호 유효시간(초)", example = "300")
    private long expiresInSec;
}
