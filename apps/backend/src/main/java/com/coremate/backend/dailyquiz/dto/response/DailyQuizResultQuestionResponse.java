package com.coremate.backend.dailyquiz.dto.response;

import com.coremate.backend.problemset.dto.response.QuestionChoiceResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "오늘의 CS 퀴즈 문항별 결과/해설 응답 DTO")
public record DailyQuizResultQuestionResponse(
        @Schema(description = "문제 ID", example = "5001")
        Long questionId,

        @Schema(description = "퀴즈 내 문제 순서", example = "1")
        Integer sortOrder,

        @Schema(description = "과목명", example = "자료구조")
        String subjectName,

        @Schema(description = "개념명", example = "완전 이진 트리")
        String conceptName,

        @Schema(description = "문제 본문")
        String questionText,

        @Schema(description = "선택지 목록")
        List<QuestionChoiceResponse> choices,

        @Schema(description = "정답", example = "B")
        String correctAnswer,

        @Schema(description = "해설")
        String explanationText,

        @Schema(description = "내가 제출한 답 (미제출 시 null)", example = "C")
        String selectedAnswer,

        @Schema(description = "정답 여부 (미제출 시 null)", example = "false")
        Boolean isCorrect
) {
}
