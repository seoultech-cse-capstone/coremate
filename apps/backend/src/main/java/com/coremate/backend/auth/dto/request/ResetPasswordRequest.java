package com.coremate.backend.auth.dto.request;

import com.coremate.backend.global.validation.PasswordPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "비밀번호 재설정 요청")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {

    @Schema(description = "인증번호 검증 후 발급받은 재설정 토큰", example = "e2b1c0d4-...")
    @NotBlank
    private String resetToken;

    @Schema(example = "N3wP@ssw0rd!", description = "대문자·소문자·숫자·특수문자(!@#$%^&*) 각 1개 이상 포함 8자 이상")
    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = PasswordPolicy.REGEXP, message = PasswordPolicy.MESSAGE)
    private String newPassword;
}
