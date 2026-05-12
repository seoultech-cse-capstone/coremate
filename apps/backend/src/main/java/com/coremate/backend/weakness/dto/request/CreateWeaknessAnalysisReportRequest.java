package com.coremate.backend.weakness.dto.request;

import com.coremate.backend.weakness.dto.type.AnalysisScopeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "약점 분석표 생성 요청 DTO")
public record CreateWeaknessAnalysisReportRequest(
        @NotEmpty
        @Size(min = 1, max = 10)
        @Schema(description = "분석에 사용할 완료 풀이 세션 ID 목록. 1~10개", example = "[9001, 9002]", requiredMode = Schema.RequiredMode.REQUIRED)
        List<Long> solveSessionIds,

        @NotNull
        @Schema(description = "분석 범위 유형", example = "CATEGORY_LV2", requiredMode = Schema.RequiredMode.REQUIRED)
        AnalysisScopeType analysisScopeType,

        @Schema(description = "과목 ID", example = "1")
        Long subjectId,

        @Schema(description = "대분류 ID", example = "10")
        Long categoryLv1Id,

        @Schema(description = "중분류 ID", example = "101")
        Long categoryLv2Id,

        @Schema(description = "개념 ID", example = "1001")
        Long conceptId,

        @Schema(description = "분석표 제목. 비우면 백엔드가 자동 생성", example = "자료구조 이진 트리 약점 분석표")
        String title,

        @Schema(description = "코인을 사용해 생성할지 여부", example = "true")
        Boolean useCoin
) {
}
