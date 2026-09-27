package com.coremate.backend.weakness.dto.type;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "약점 분석표 상태")
public enum AnalysisReportStatus {
    @Schema(description = "분석 요청됨")
    REQUESTED,

    @Schema(description = "AI 분석 생성 중")
    GENERATING,

    @Schema(description = "분석 완료")
    COMPLETED,

    @Schema(description = "분석 실패")
    FAILED,

    @Schema(description = "삭제됨")
    DELETED
}
