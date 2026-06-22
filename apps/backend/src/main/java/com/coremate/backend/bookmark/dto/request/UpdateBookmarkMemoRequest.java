package com.coremate.backend.bookmark.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Update bookmark memo request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBookmarkMemoRequest {
    @Schema(example = "Update memo")
    @Size(max = 2000)
    private String memo;
}
