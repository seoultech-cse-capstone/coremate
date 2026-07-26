package com.coremate.backend.friend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Send friend request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendFriendRequest {
    @Schema(example = "99")
    @NotNull
    @Positive
    private Long receiverId;
}
