package com.coremate.backend.progress.dto.response;

import com.coremate.backend.progress.dto.type.ProgressScopeType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "범위별 문제집 진행률. 하위 개념 중 합격(PASS)한 개념 비율이다")
public record ProblemSetPassRateResponse(

        @Schema(description = "집계 범위", example = "CATEGORY_LV2")
        ProgressScopeType scopeType,

        @Schema(description = "범위 ID(과목/대분류/중분류 ID)", example = "9203")
        Long targetId,

        @Schema(description = "범위 하위의 활성 개념 수", example = "4")
        long totalConceptCount,

        @Schema(description = "그중 합격(전 문제 정답)한 개념 수", example = "1")
        long passedConceptCount,

        @Schema(description = "진행률(%). 0~100 정수", example = "25")
        int progressRate,

        @Schema(description = "완료 여부. 진행률이 100일 때만 true", example = "false")
        boolean completed
) {
}
