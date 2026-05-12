package com.coremate.backend.weakness.dto.response;

import com.coremate.backend.solve.dto.type.SolveSessionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "약점 분석 대상 후보 문제집 응답 DTO")
public record EligibleSolveSessionResponse(
        @Schema(description = "풀이 세션 ID", example = "9001")
        Long solveSessionId,

        @Schema(description = "문제집 제목", example = "완전 이진 트리 개념 문제집")
        String title,

        @Schema(description = "풀이 유형", example = "CONCEPT_PRACTICE")
        SolveSessionType sessionType,

        @Schema(description = "과목 ID", example = "1")
        Long subjectId,

        @Schema(description = "과목명", example = "자료구조")
        String subjectName,

        @Schema(description = "대분류 ID", example = "10")
        Long categoryLv1Id,

        @Schema(description = "대분류명", example = "트리")
        String categoryLv1Name,

        @Schema(description = "중분류 ID", example = "101")
        Long categoryLv2Id,

        @Schema(description = "중분류명", example = "이진 트리")
        String categoryLv2Name,

        @Schema(description = "개념 ID", example = "1001")
        Long conceptId,

        @Schema(description = "개념명", example = "완전 이진 트리")
        String conceptName,

        @Schema(description = "총 문제 수", example = "5")
        Integer totalQuestionCount,

        @Schema(description = "틀린 문항 수", example = "2")
        Integer wrongCount,

        @Schema(description = "오래 머문 문제 수", example = "1")
        Integer longTimeCount,

        @Schema(description = "힌트 사용 문제 수", example = "1")
        Integer hintUsedCount,

        @Schema(description = "완료 시간")
        LocalDateTime completedAt,

        @Schema(description = "현재 필터 기준 선택 가능 여부", example = "true")
        Boolean eligible,

        @Schema(description = "선택 불가 또는 안내 메시지", example = "틀린 문제가 적어 분석 우선순위가 낮습니다.")
        String warningMessage
) {
}
