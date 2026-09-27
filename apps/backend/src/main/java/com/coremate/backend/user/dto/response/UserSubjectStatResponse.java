package com.coremate.backend.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "사용자 과목 통계 응답")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSubjectStatResponse {
    @Schema(example = "1")
    private Long subjectId;

    @Schema(example = "120")
    private Integer solvedQuestionCount;

    @Schema(example = "90")
    private Integer correctQuestionCount;

    @Schema(example = "30")
    private Integer wrongQuestionCount;

    @Schema(description = "과목별 학습 달성률(%). 0.0 ~ 100.0", example = "30.0")
    private Double achievementRate;
}
