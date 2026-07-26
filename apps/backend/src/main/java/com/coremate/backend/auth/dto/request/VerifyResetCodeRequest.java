package com.coremate.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "비밀번호 재설정 인증번호 검증 요청")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyResetCodeRequest {

    @Schema(example = "user@example.com")
    @Email
    @NotBlank
    private String email;

    @Schema(example = "123456")
    @NotBlank
    @Pattern(regexp = "^[0-9]{6}$", message = "인증번호 형식이 올바르지 않습니다")
    private String code;
}
