package com.coremate.backend.support.dto.request;

import com.coremate.backend.support.entity.type.InquiryCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "문의 등록 요청")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInquiryRequest {

    @Schema(example = "PROBLEM_SET")
    @NotNull
    private InquiryCategory category;

    @Schema(example = "문제집 화면 관련 문의")
    @NotBlank
    @Size(max = 50)
    private String title;

    @Schema(example = "특정 문제집을 열면 로딩이 멈춥니다.")
    @NotBlank
    @Size(max = 300)
    private String body;
}
