package com.coremate.backend.support.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "버그 신고 요청")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBugReportRequest {

    @Schema(example = "문제를 풀고 제출하면 앱이 종료됩니다.")
    @NotBlank
    @Size(max = 1000)
    private String body;
}
