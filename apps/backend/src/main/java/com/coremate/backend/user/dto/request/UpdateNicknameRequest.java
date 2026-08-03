package com.coremate.backend.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "닉네임 변경 요청")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNicknameRequest {
    @Schema(example = "코어", description = "닉네임 (최대 6글자)")
    @NotBlank
    @Size(max = 6, message = "닉네임은 최대 6글자까지 가능합니다")
    private String nickname;
}
