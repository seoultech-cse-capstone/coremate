package com.coremate.backend.weakness.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "약점 판단 근거가 된 문제 풀이 로그 응답 DTO")
public record WeaknessEvidenceLogResponse(
        @Schema(description = "근거 ID", example = "40001")
        Long evidenceId,

        @Schema(description = "풀이 로그 ID", example = "10001")
        Long solveLogId,

        @Schema(description = "문제 ID", example = "5001")
        Long questionId,

        @Schema(description = "문제 본문")
        String questionText,

        @Schema(description = "근거 유형. WRONG, LONG_TIME, HINT_USED, ANSWER_CHANGED", example = "WRONG")
        String evidenceType,

        @Schema(description = "풀이 시간 초", example = "80")
        Integer timeSpentSec,

        @Schema(description = "예상 풀이 시간 초", example = "45")
        Integer expectedSolveSec,

        @Schema(description = "힌트 사용 횟수", example = "2")
        Integer hintCount,

        @Schema(description = "사용자 선택 답", example = "A")
        String selectedAnswer,

        @Schema(description = "사용자가 고른 선지의 문장")
        String selectedAnswerText,

        @Schema(description = "정답", example = "B")
        String correctAnswer,

        @Schema(description = "정답 선지의 문장")
        String correctAnswerText,

        @Schema(description = "그 선지를 고른 학습자가 무엇을 잘못 봤는지 한 문장. 문항 진단 프로필에서 온다")
        String selectedAnswerReason,

        @Schema(description = "그 선지가 드러내는 오개념 제목. common_mistake.mistake_code 로 잇는다")
        String misconceptionTitle,

        @Schema(description = "정답 여부", example = "false")
        Boolean isCorrect,

        @Schema(description = "AI가 이 근거를 진단의 직접 근거로 지목했는지. AI 응답의 evidence_refs 에서 온다",
                example = "true")
        Boolean citedByAi
) {
}
