package com.coremate.backend.user.dto.response;

import com.coremate.backend.user.entity.SocialProvider;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "관리자 회원 찾기 항목")
public record AdminUserResponse(
        @Schema(example = "42") Long userId,
        @Schema(example = "user@example.com", description = "소셜 가입에서 이메일을 받지 못했으면 null") String email,
        @Schema(example = "데모용주") String nickname,
        @Schema(example = "ACTIVE") UserStatus status,
        @Schema(example = "[\"KAKAO\"]", description = "연결된 소셜 계정. 이메일로만 가입했으면 비어 있다")
        List<SocialProvider> socialProviders,
        @Schema(example = "2026-10-26T19:10:11") LocalDateTime createdAt,
        @Schema(example = "2026-11-01T08:00:00") LocalDateTime lastLoginAt
) {

    public static AdminUserResponse of(User user, List<SocialProvider> socialProviders) {
        return new AdminUserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getNickname(),
                user.getStatus(),
                socialProviders,
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }
}
