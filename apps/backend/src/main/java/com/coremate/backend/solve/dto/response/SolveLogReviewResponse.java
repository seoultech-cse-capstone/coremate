package com.coremate.backend.solve.dto.response;

import com.coremate.backend.problemset.dto.response.QuestionChoiceResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "문제별 풀이 기록/해설 응답 DTO")
public record SolveLogReviewResponse(
        @Schema(description = "풀이 로그 ID", example = "10001")
        Long solveLogId,

        @Schema(description = "문제 ID", example = "5001")
        Long questionId,

        @Schema(description = "문제 순서", example = "1")
        Integer questionOrder,

        @Schema(description = "문제 본문")
        String questionText,

        @Schema(description = "선택지 목록")
        List<QuestionChoiceResponse> choices,

        @Schema(description = "사용자가 선택한 답", example = "B")
        String selectedAnswer,

        @Schema(description = "정답", example = "B")
        String correctAnswer,

        @Schema(description = "정답 여부", example = "true")
        Boolean isCorrect,

        @Schema(description = "문제 해설")
        String explanationText,

        @Schema(description = "풀이 시간 초", example = "38")
        Integer timeSpentSec,

        @Schema(description = "예상 풀이 시간 초", example = "45")
        Integer expectedSolveSec,

        @Schema(description = "힌트 사용 여부", example = "true")
        Boolean hintUsed,

        @Schema(description = "힌트 사용 횟수", example = "1")
        Integer hintCount,

        @Schema(description = "답 변경 횟수", example = "1")
        Integer optionChangeCount,

        @Schema(description = "답 변경 여부", example = "true")
        Boolean isAnswerChanged,

        @Schema(description = "풀이 당시 즐겨찾기 여부", example = "false")
        Boolean isBookmarked,

        @Schema(description = "풀이 완료 시간")
        LocalDateTime solvedAt
) {
}
