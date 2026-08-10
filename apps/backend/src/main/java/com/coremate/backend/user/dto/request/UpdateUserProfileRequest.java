package com.coremate.backend.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "사용자 프로필 수정 요청")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequest {
    @Schema(example = "코어", description = "닉네임 (최대 6글자)")
    @Size(max = 6, message = "닉네임은 최대 6글자까지 가능합니다")
    private String nickname;

    @Schema(example = "School A")
    @Size(max = 100)
    private String schoolName;

    @Schema(example = "student@school.edu")
    @Email
    private String schoolEmail;
}
