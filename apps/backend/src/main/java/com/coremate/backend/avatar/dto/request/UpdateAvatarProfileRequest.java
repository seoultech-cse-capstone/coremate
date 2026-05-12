package com.coremate.backend.avatar.dto.request;

import com.coremate.backend.avatar.dto.type.AvatarGenderType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "아바타 프로필 수정 요청")
public record UpdateAvatarProfileRequest(
        @Schema(example = "FEMALE") AvatarGenderType avatarGender,
        @Schema(example = "BASIC_01") @Size(max = 50) String baseAvatarType
) {
}
