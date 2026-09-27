package com.coremate.backend.friend.dto.request;

import com.coremate.backend.friend.dto.type.FriendStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Respond to friend request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespondFriendRequest {
    @Schema(example = "123")
    @NotNull
    @Positive
    private Long friendId;

    @Schema(example = "ACCEPTED")
    @NotNull
    private FriendStatus status;
}
