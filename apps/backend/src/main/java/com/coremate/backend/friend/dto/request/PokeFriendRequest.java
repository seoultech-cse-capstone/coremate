package com.coremate.backend.friend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Poke friend request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PokeFriendRequest {
    @Schema(example = "99")
    @NotNull
    private Long toUserId;

    @Schema(example = "POKE")
    @NotBlank
    private String messageType;
}
