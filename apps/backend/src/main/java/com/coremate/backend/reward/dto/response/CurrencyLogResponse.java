package com.coremate.backend.reward.dto.response;

import com.coremate.backend.reward.entity.type.CurrencyReason;
import com.coremate.backend.reward.entity.type.CurrencyTransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "재화 로그 응답")
public record CurrencyLogResponse(
        @Schema(example = "1") Long currencyLogId,
        @Schema(example = "42") Long userId,
        @Schema(example = "-100") Integer amount,
        @Schema(example = "250") Integer balanceAfter,
        @Schema(example = "SPEND") CurrencyTransactionType transactionType,
        @Schema(example = "RANDOM_BOX_PURCHASE") CurrencyReason reason,
        @Schema(example = "SHOP") String referenceType,
        @Schema(example = "10") Long referenceId,
        @Schema(example = "2026-05-12T09:10:11") LocalDateTime createdAt
) {
}
