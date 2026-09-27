package com.coremate.backend.progress.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "과목별 학습 진행률 응답 DTO")
public record SubjectProgressResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "과목 ID", example = "1")
        Long subjectId,

        @Schema(description = "과목명", example = "자료구조")
        String subjectName,

        @Schema(description = "푼 문제 수", example = "120")
        Integer solvedQuestionCount,

        @Schema(description = "맞힌 문제 수", example = "96")
        Integer correctQuestionCount,

        @Schema(description = "틀린 문제 수", example = "24")
        Integer wrongQuestionCount,

        @Schema(description = "완료한 개념 수", example = "12")
        Integer completedConceptCount,

        @Schema(description = "전체 개념 수", example = "40")
        Integer totalConceptCount,

        @Schema(description = "완료한 챕터 모의고사 수", example = "3")
        Integer completedCategoryMockCount,

        @Schema(description = "완료한 과목 모의고사 수", example = "1")
        Integer completedSubjectMockCount,

        @Schema(description = "육각형 스탯 점수", example = "72")
        Integer statScore,

        @Schema(description = "학습 달성률", example = "30.0")
        Double achievementRate
) {
}
