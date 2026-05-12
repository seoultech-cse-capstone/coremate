package com.coremate.backend.bookmark.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "북마크한 문제 응답")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkedQuestionResponse {
    @Schema(example = "101")
    private Long questionId;

    @Schema(example = "55")
    private Long conceptId;

    @Schema(example = "MCQ_4")
    private String questionFormat;

    @Schema(example = "SINGLE")
    private String questionType;

    @Schema(example = "3")
    private Integer difficultyLevel;

    @Schema(example = "2026-05-12T09:10:11")
    private LocalDateTime bookmarkedAt;
}
