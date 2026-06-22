package com.coremate.backend.problemset.dto.response;

import com.coremate.backend.problemset.dto.type.QuestionFormat;
import com.coremate.backend.problemset.dto.type.QuestionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "문제집 내 개별 문제 응답 DTO")
public record ProblemSetQuestionResponse(
        @Schema(description = "문제 ID", example = "5001")
        Long questionId,

        @Schema(description = "기준 개념 ID", example = "1001")
        Long conceptId,

        @Schema(description = "기준 개념명", example = "완전 이진 트리")
        String conceptName,

        @Schema(description = "문제 순서", example = "1")
        Integer questionOrder,

        @Schema(description = "문제 형식", example = "MCQ_4")
        QuestionFormat questionFormat,

        @Schema(description = "문제 유형", example = "definition")
        QuestionType questionType,

        @Schema(description = "문제 본문", example = "완전 이진 트리에 대한 설명으로 가장 적절한 것은?")
        String questionText,

        @Schema(description = "객관식 선택지")
        List<QuestionChoiceResponse> choices,

        @Schema(description = "난이도 1~5", example = "2")
        Integer difficultyLevel,

        @Schema(description = "예상 풀이 시간 초", example = "45")
        Integer expectedSolveSec,

        @Schema(description = "힌트 존재 여부", example = "true")
        Boolean hasHint,

        @Schema(description = "현재 사용자의 즐겨찾기 여부", example = "false")
        Boolean isBookmarked
) {
}