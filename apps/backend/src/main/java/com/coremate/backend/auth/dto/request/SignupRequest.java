package com.coremate.backend.auth.dto.request;

import com.coremate.backend.global.validation.PasswordPolicy;
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

    @Schema(example = "P@ssw0rd!", description = "대문자·소문자·숫자·특수문자(!@#$%^&*) 각 1개 이상 포함 8자 이상")
    @NotBlank
    @Pattern(regexp = PasswordPolicy.REGEXP, message = PasswordPolicy.MESSAGE)
    private String password;

    @Schema(example = "코어", description = "닉네임 (최대 6글자)")
    @NotBlank
    @Size(max = 6, message = "닉네임은 최대 6글자까지 가능합니다")
    private String nickname;

    @Schema(example = "홍길동", description = "이름 (한글 또는 영문만, 특수문자·숫자·공백 불가)")
    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = "^[가-힣a-zA-Z]+$", message = "이름은 한글 또는 영문만 사용할 수 있습니다")
    private String name;

    @Schema(example = "01012345678")
    @NotBlank
    @Pattern(regexp = "^01[0-9]{8,9}$", message = "휴대폰 번호 형식이 올바르지 않습니다")
    private String phoneNumber;
}
