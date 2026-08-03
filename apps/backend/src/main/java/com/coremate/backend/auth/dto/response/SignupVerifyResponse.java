package com.coremate.backend.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "회원가입 이메일 인증번호 검증 응답")
@Getter
@Builder
public class SignupVerifyResponse {

    @Schema(description = "인증 완료 여부", example = "true")
    private boolean verified;

    @Schema(description = "인증 완료 상태 유효시간(초). 이 시간 안에 회원가입을 완료해야 한다.", example = "1800")
    private long expiresInSec;
}
