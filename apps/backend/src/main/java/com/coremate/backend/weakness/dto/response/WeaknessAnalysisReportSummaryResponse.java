package com.coremate.backend.weakness.dto.response;

import com.coremate.backend.weakness.dto.type.AnalysisReportStatus;
import com.coremate.backend.weakness.dto.type.AnalysisScopeType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "생성된 약점 분석표 목록 응답 DTO")
public record WeaknessAnalysisReportSummaryResponse(
        @Schema(description = "약점 분석표 ID", example = "20001")
        Long analysisReportId,

        @Schema(description = "분석표 제목", example = "자료구조 이진 트리 약점 분석표")
        String title,

        @Schema(description = "분석 범위 유형", example = "CATEGORY_LV2")
        AnalysisScopeType analysisScopeType,

        @Schema(description = "분석 범위명", example = "자료구조 > 트리 > 이진 트리")
        String analysisScopeNameSnapshot,

        @Schema(description = "분석표 상태", example = "COMPLETED")
        AnalysisReportStatus status,

        @Schema(description = "분석에 사용한 문제집 수", example = "2")
        Integer selectedSessionCount,

        @Schema(description = "분석 대상 전체 문제 수", example = "10")
        Integer totalQuestionCount,

        @Schema(description = "분석 대상 중 틀린 문제 수", example = "4")
        Integer totalWrongCount,

        @Schema(description = "이 분석표가 짚어낸 약점 개념 수. 목록 화면의 '발견한 약점'이 쓰는 값이며, "
                + "틀린 문제 수(totalWrongCount)와는 다른 지표다.", example = "3")
        Integer weaknessConceptCount,

        @Schema(description = "생성에 사용한 코인", example = "100")
        Integer coinCost,

        @Schema(description = "생성 완료 시간")
        LocalDateTime generatedAt,

        @Schema(description = "PDF 제공 여부", example = "true")
        Boolean pdfAvailable
) {
}
