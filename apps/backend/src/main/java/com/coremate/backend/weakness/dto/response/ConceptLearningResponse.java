package com.coremate.backend.weakness.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 약점 개념의 학습 콘텐츠(개념별 진단 프로필에서 결합). 약점 확인과 개념 학습을 한 화면에서
 * 제공하기 위한 영역으로, 승인된 프로필이 없으면 null 이다.
 */
@Schema(description = "약한 개념 학습 내용(개념 진단 프로필 결합)")
public record ConceptLearningResponse(

        @Schema(description = "개념 핵심 요약")
        String conceptCore,

        @Schema(description = "원리까지 풀어 쓴 개념 설명")
        String conceptExplanation,

        @Schema(description = "핵심 포인트 목록")
        List<String> keyPoints,

        @Schema(description = "혼동하기 쉬운 개념과의 비교")
        List<ConfusionComparison> confusionComparisons,

        @Schema(description = "선수 개념 목록")
        List<String> prerequisiteConcepts,

        @Schema(description = "개념 확인용 예시")
        String learningExample,

        @Schema(description = "흔한 오개념과 교정 포인트")
        String misconceptionNotes,

        @Schema(description = "오답 반복 시 되짚을 기초 복습 포인트")
        List<String> basicReviewPoints
) {

    @Schema(description = "혼동 개념 비교 항목")
    public record ConfusionComparison(

            @Schema(description = "비교 대상 개념명", example = "포화 이진 트리")
            String comparedWith,

            @Schema(description = "결정적 차이", example = "포화는 모든 리프가 같은 레벨에 있어야 한다.")
            String difference
    ) {
    }
}
