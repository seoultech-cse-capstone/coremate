package com.coremate.backend.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "사용자 요약 응답")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {
    @Schema(example = "42")
    private Long userId;

    @Schema(example = "coremate")
    private String nickname;

    @Schema(example = "5")
    private Integer level;

    @Schema(example = "https://cdn.example.com/avatar.png")
    private String avatarImageUrl;
}
