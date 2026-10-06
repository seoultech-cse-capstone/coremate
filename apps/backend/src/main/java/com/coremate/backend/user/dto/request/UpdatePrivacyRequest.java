package com.coremate.backend.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "공개 범위 변경 요청")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePrivacyRequest {
    @Schema(example = "false", description = "친구에게 학습 기록(과목별 스탯·Learning History) 공개 여부")
    @NotNull
    private Boolean learningPublic;
}
