package com.coremate.backend.friend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Friend poke response")
public record FriendPokeResponse(
        @Schema(example = "1") Long pokeId,
        @Schema(example = "42") Long fromUserId,
        @Schema(example = "99") Long toUserId,
        @Schema(example = "POKE") String messageType,
        @Schema(example = "false") Boolean isRead,
        @Schema(example = "2026-05-12T09:10:11") LocalDateTime createdAt
) {
}
