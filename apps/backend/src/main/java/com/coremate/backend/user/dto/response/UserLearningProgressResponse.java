package com.coremate.backend.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "사용자 학습 진행도 응답")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLearningProgressResponse {
    @Schema(example = "42")
    private Long userId;

    @Schema(example = "12")
    private Integer completedConceptCount;

    @Schema(example = "30")
    private Integer completedCategoryMockCount;

    @Schema(example = "5")
    private Integer completedSubjectMockCount;

    @Schema(example = "0.75")
    private String overallAchievementRate;
}
