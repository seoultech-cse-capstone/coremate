package com.coremate.backend.shop.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "랜덤박스 오픈 응답")
public record RandomBoxOpenResponse(
        @Schema(example = "200") Long randomBoxOpenLogId,
        @Schema(example = "42") Long userId,
        @Schema(example = "3") Long randomBoxId,
        @Schema(example = "ITEM") String rewardType,
        @Schema(example = "1") Long rewardItemId,
        @Schema(example = "50") Integer rewardCoinAmount,
        @Schema(example = "EPIC", description = "보상 등급. 아이템은 rarity(COMMON/RARE/EPIC/LEGENDARY), 코인 보상은 COIN") String rewardRarity,
        @Schema(example = "2026-05-12T09:10:11") LocalDateTime openedAt
) {
}
