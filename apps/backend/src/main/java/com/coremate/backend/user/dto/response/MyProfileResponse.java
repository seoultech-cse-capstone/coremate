package com.coremate.backend.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "내 프로필 응답")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyProfileResponse {
    @Schema(example = "42")
    private Long userId;

    @Schema(example = "user@example.com")
    private String email;

    @Schema(example = "coremate")
    private String nickname;

    @Schema(example = "USER")
    private String role;

    @Schema(example = "1")
    private Integer level;

    @Schema(example = "1200")
    private Integer totalXp;

    @Schema(example = "School A")
    private String schoolName;

    @Schema(example = "student@school.edu")
    private String schoolEmail;

    @Schema(example = "false")
    private Boolean schoolVerified;

    @Schema(example = "ACTIVE")
    private String status;

    @Schema(example = "2026-05-12T09:10:11")
    private LocalDateTime createdAt;

    @Schema(example = "2026-05-12T09:10:11")
    private LocalDateTime lastLoginAt;
}
