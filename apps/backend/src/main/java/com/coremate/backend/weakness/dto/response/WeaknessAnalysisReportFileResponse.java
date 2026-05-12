package com.coremate.backend.weakness.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "약점 분석표 파일 응답 DTO")
public record WeaknessAnalysisReportFileResponse(
        @Schema(description = "약점 분석표 ID", example = "20001")
        Long analysisReportId,

        @Schema(description = "앱 내 뷰어 콘텐츠 타입", example = "PDF")
        String viewerContentType,

        @Schema(description = "PDF 다운로드 URL", example = "https://cdn.coremate.app/reports/20001.pdf")
        String pdfFileUrl,

        @Schema(description = "S3 등 파일 저장 키", example = "reports/weakness/20001.pdf")
        String pdfStorageKey,

        @Schema(description = "다운로드 가능 여부", example = "true")
        Boolean downloadable
) {
}
