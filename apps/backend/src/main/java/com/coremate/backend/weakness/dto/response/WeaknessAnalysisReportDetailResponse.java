package com.coremate.backend.weakness.dto.response;

import com.coremate.backend.weakness.dto.type.AnalysisReportStatus;
import com.coremate.backend.weakness.dto.type.AnalysisScopeType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "약점 분석표 상세 응답 DTO")
public record WeaknessAnalysisReportDetailResponse(
        @Schema(description = "약점 분석표 ID", example = "20001")
        Long analysisReportId,

        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "분석표 제목", example = "자료구조 이진 트리 약점 분석표")
        String title,

        @Schema(description = "분석 범위 유형", example = "CATEGORY_LV2")
        AnalysisScopeType analysisScopeType,

        @Schema(description = "분석 범위명", example = "자료구조 > 트리 > 이진 트리")
        String analysisScopeNameSnapshot,

        @Schema(description = "분석표 짧은 요약")
        String summaryText,

        @Schema(description = "AI가 생성한 전체 분석 본문")
        String analysisText,

        @Schema(description = "분석 본문 저장 형식", example = "MARKDOWN")
        String reportFormat,

        @Schema(description = "선택한 문제집 수", example = "2")
        Integer selectedSessionCount,

        @Schema(description = "분석 대상 전체 문제 수", example = "10")
        Integer totalQuestionCount,

        @Schema(description = "분석 대상 오답 수", example = "4")
        Integer totalWrongCount,

        @Schema(description = "오래 머문 문제 수", example = "2")
        Integer totalLongTimeCount,

        @Schema(description = "힌트 사용 문제 수", example = "3")
        Integer totalHintUsedCount,

        @Schema(description = "생성 비용 코인", example = "100")
        Integer coinCost,

        @Schema(description = "분석표 상태", example = "COMPLETED")
        AnalysisReportStatus status,

        @Schema(description = "생성 요청 시간")
        LocalDateTime requestedAt,

        @Schema(description = "생성 완료 시간")
        LocalDateTime generatedAt,

        @Schema(description = "분석에 사용한 문제집 목록")
        List<EligibleSolveSessionResponse> selectedSessions,

        @Schema(description = "약점 개념 분석 목록")
        List<WeaknessConceptResponse> weaknessConcepts
) {
}
