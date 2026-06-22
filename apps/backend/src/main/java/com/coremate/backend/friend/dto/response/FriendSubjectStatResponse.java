package com.coremate.backend.friend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Friend subject stat response")
public record FriendSubjectStatResponse(
        @Schema(example = "1") Long subjectId,
        @Schema(example = "120") Integer solvedQuestionCount,
        @Schema(example = "90") Integer correctQuestionCount,
        @Schema(example = "30") Integer wrongQuestionCount,
        @Schema(example = "0.75") String achievementRate
) {
}
