package com.coremate.backend.problemset.dto.response;

import com.coremate.backend.problemset.dto.type.ProblemSetType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "문제집 상세 응답 DTO")
public record ProblemSetDetailResponse(
        @Schema(description = "문제집 유형", example = "CONCEPT_PRACTICE")
        ProblemSetType problemSetType,

        @Schema(description = "대상 유형", example = "CONCEPT")
        String targetType,

        @Schema(description = "대상 ID", example = "1001")
        Long targetId,

        @Schema(description = "문제집 제목", example = "완전 이진 트리 개념 문제집")
        String title,

        @Schema(description = "문제집 설명")
        String description,

        @Schema(description = "총 문제 수", example = "5")
        Integer totalQuestionCount,

        @Schema(description = "제한 시간 초. 제한이 없으면 null")
        Integer timeLimitSec,

        @Schema(description = "문제별 즉시 피드백 여부. 개념 문제집은 true, 모의고사는 false", example = "true")
        Boolean immediateFeedback,

        @Schema(description = "문제 목록")
        List<ProblemSetQuestionResponse> questions
) {
}
