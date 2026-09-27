package com.coremate.backend.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "회원가입 이메일 인증번호 발송 응답")
@Getter
@Builder
public class SignupCodeResponse {

    @Schema(description = "인증번호 유효시간(초)", example = "300")
    private long expiresInSec;
}
