package com.coremate.backend.friend.dto.response;

import com.coremate.backend.friend.dto.type.FriendStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "친구 요청 응답")
public record FriendRequestResponse(
        @Schema(example = "123") Long friendId,
        @Schema(example = "42") Long requesterId,
        @Schema(example = "데모용주") String requesterNickname,
        @Schema(example = "99") Long receiverId,
        @Schema(example = "corefriend") String receiverNickname,
        @Schema(example = "REQUESTED") FriendStatus status,
        @Schema(example = "RECEIVED") String direction,
        @Schema(example = "2026-05-12T09:10:11") LocalDateTime createdAt
) {
}
