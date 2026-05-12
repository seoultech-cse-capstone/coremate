package com.coremate.backend.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Signup response")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupResponse {
    @Schema(example = "42")
    private Long userId;

    @Schema(example = "user@example.com")
    private String email;

    @Schema(example = "coremate")
    private String nickname;
}
