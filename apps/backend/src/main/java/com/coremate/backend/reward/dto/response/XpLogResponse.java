package com.coremate.backend.reward.dto.response;

import com.coremate.backend.reward.dto.type.XpReasonType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "경험치 로그 응답")
public record XpLogResponse(
        @Schema(example = "1") Long xpLogId,
        @Schema(example = "42") Long userId,
        @Schema(example = "10") Integer amount,
        @Schema(example = "1200") Integer totalXpAfter,
        @Schema(example = "SOLVE_QUESTION") XpReasonType reason,
        @Schema(example = "QUESTION") String referenceType,
        @Schema(example = "101") Long referenceId,
        @Schema(example = "2026-05-12T09:10:11") LocalDateTime createdAt
) {
}
