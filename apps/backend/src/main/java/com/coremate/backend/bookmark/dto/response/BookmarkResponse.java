package com.coremate.backend.bookmark.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "북마크 응답")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkResponse {
    @Schema(example = "10")
    private Long bookmarkId;

    @Schema(example = "42")
    private Long userId;

    @Schema(example = "101")
    private Long questionId;

    @Schema(example = "Review this later")
    private String memo;

    @Schema(example = "2026-05-12T09:10:11")
    private LocalDateTime createdAt;

    @Schema(example = "2026-05-12T09:10:11")
    private LocalDateTime updatedAt;
}
