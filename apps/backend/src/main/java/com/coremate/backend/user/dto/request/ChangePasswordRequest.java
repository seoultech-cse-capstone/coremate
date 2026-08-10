package com.coremate.backend.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "로그인 상태 비밀번호 변경 요청")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    @Schema(example = "P@ssw0rd!", description = "현재 비밀번호")
    @NotBlank
    private String currentPassword;

    @Schema(example = "N3wP@ssw0rd!", description = "새 비밀번호")
    @NotBlank
    private String newPassword;
}
