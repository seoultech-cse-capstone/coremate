package com.coremate.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "회원 탈퇴 요청")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawRequest {

    @Schema(description = "본인 확인용 현재 비밀번호", example = "P@ssw0rd!")
    @NotBlank
    private String password;
}
