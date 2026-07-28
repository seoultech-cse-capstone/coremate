package com.coremate.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "회원가입 요청")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {
    @Schema(example = "user@example.com")
    @Email
    @NotBlank
    private String email;

    @Schema(example = "P@ssw0rd!")
    @NotBlank
    private String password;

    @Schema(example = "coremate")
    @NotBlank
    @Size(max = 50)
    private String nickname;

    @Schema(example = "홍길동")
    @NotBlank
    @Size(max = 50)
    private String name;

    @Schema(example = "01012345678")
    @NotBlank
    @Pattern(regexp = "^01[0-9]{8,9}$", message = "휴대폰 번호 형식이 올바르지 않습니다")
    private String phoneNumber;
}
