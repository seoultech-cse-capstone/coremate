package com.coremate.backend.dailyquiz.dto.response;

import com.coremate.backend.problemset.dto.response.QuestionChoiceResponse;
import com.coremate.backend.problemset.dto.type.QuestionFormat;
import com.coremate.backend.problemset.dto.type.QuestionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "오늘의 CS 퀴즈 개별 문제 응답 DTO")
public record DailyQuizQuestionResponse(
        @Schema(description = "문제 ID", example = "5001")
        Long questionId,

        @Schema(description = "과목 ID", example = "1")
        Long subjectId,

        @Schema(description = "과목명", example = "자료구조")
        String subjectName,

        @Schema(description = "개념 ID", example = "1001")
        Long conceptId,

        @Schema(description = "개념명", example = "완전 이진 트리")
        String conceptName,

        @Schema(description = "퀴즈 내 문제 순서", example = "1")
        Integer sortOrder,

        @Schema(description = "문제 형식", example = "MCQ_4")
        QuestionFormat questionFormat,

        @Schema(description = "문제 유형", example = "definition")
        QuestionType questionType,

        @Schema(description = "문제 본문")
        String questionText,

        @Schema(description = "선택지 목록")
        List<QuestionChoiceResponse> choices,

        @Schema(description = "난이도", example = "2")
        Integer difficultyLevel,

        @Schema(description = "예상 풀이 시간 초", example = "45")
        Integer expectedSolveSec,

        @Schema(description = "힌트 존재 여부", example = "true")
        Boolean hasHint
) {
}