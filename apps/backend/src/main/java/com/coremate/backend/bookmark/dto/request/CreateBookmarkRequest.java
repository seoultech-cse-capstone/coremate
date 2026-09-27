package com.coremate.backend.bookmark.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Create bookmark request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookmarkRequest {
    @Schema(example = "101")
    @NotNull
    private Long questionId;

    @Schema(example = "55")
    private Long sourceSolveSessionId;

    @Schema(example = "Review this later")
    @Size(max = 2000)
    private String memo;
}
