package com.coremate.backend.learninghistory.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Learning History 응답 DTO. 사용자 단위 누적 지표 3개. 기록이 없으면 모두 0")
public record LearningHistoryResponse(
        @Schema(description = "모의고사(과목/대분류/중분류) 총 풀이 시간(초). 개념 문제집 제외", example = "1800")
        int totalMockSolveTimeSec,

        @Schema(description = "개념 문제집에서 맞힌 서로 다른(distinct) 문제 수", example = "42")
        int conceptCorrectQuestionCount,

        @Schema(description = "PASS(전 문제 정답)로 통과한 개념 수", example = "7")
        int completedConceptCount
) {
}
