package com.coremate.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "본인 확인용 현재 비밀번호. 소셜 로그인으로만 가입한 계정(내 프로필 hasPassword=false)은 비워 보낸다.",
            example = "P@ssw0rd!")
    private String password;
}
