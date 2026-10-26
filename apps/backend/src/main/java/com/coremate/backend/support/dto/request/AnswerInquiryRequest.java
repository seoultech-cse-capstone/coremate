package com.coremate.backend.support.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "문의 답변 등록 요청(관리자)")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerInquiryRequest {

    @Schema(example = "확인 결과 해당 문제집은 수정되었습니다. 앱을 최신 버전으로 업데이트해 주세요.")
    @NotBlank
    @Size(max = 1000)
    private String answer;
}
