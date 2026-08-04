package com.coremate.backend.weakness.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "AI가 약점으로 판단한 개념별 분석 결과 DTO")
public record WeaknessConceptResponse(
        @Schema(description = "약점 개념 분석 ID", example = "30001")
        Long analysisConceptId,

        @Schema(description = "개념 ID", example = "1001")
        Long conceptId,

        @Schema(description = "개념명", example = "완전 이진 트리")
        String conceptName,

        @Schema(description = "약점 점수", example = "4")
        Integer weaknessScore,

        @Schema(description = "근거 충분도", example = "0.84")
        BigDecimal evidenceStrength,

        @Schema(description = "최종 약점 유형", example = "APPLICATION_GAP")
        String finalType,

        @Schema(description = "최종 약점 유형 출처", example = "BOTH")
        String finalTypeSource,

        @Schema(description = "보조 신호 목록", example = "[\"CONCEPT_RELATION_CONFUSION\"]")
        List<String> secondarySignals,

        @Schema(description = "원인 코드", example = "RELATIONSHIP_MAPPING_GAP")
        String causeCode,

        @Schema(description = "분석 상태", example = "SUPPORTED")
        String analysisStatus,

        @Schema(description = "혼동한 개념 관계", example = "완전 이진 트리와 포화 이진 트리의 조건")
        String confusedRelation,

        @Schema(description = "약점 진단")
        String diagnosis,

        @Schema(description = "관련 풀이 기록 요약")
        String evidenceSummary,

        @Schema(description = "해설보다 더 자세한 개념 설명")
        String detailedExplanationText,

        @Schema(description = "개념 학습 팁")
        String conceptTip,

        @Schema(description = "약한 개념 학습 내용(개념 진단 프로필). 승인된 프로필이 없으면 null")
        ConceptLearningResponse conceptLearning,

        @Schema(description = "약점 판단 근거 로그 목록")
        List<WeaknessEvidenceLogResponse> evidenceLogs
) {
}
