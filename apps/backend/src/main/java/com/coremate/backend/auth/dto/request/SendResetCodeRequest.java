package com.coremate.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "비밀번호 재설정 인증번호 발송 요청")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendResetCodeRequest {

    @Schema(example = "user@example.com")
    @Email
    @NotBlank
    private String email;
}
