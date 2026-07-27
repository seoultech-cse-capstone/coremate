package com.coremate.backend.problemset.dto.response;

import com.coremate.backend.problemset.dto.type.PassStatus;
import com.coremate.backend.problemset.dto.type.ProblemSetSolveStatus;
import com.coremate.backend.problemset.dto.type.ProblemSetType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "문제집 요약 응답 DTO. 완료 기록이 있으면 최고 점수 회차 기준으로 총 문제 수/정답 수/합격 여부를 반환한다.")
public record ProblemSetSummaryResponse(
        @Schema(description = "문제집 유형", example = "CONCEPT_PRACTICE")
        ProblemSetType problemSetType,

        @Schema(description = "대상 유형", example = "CONCEPT")
        String targetType,

        @Schema(description = "대상 ID (개념 문제집이면 conceptId)", example = "1001")
        Long targetId,

        @Schema(description = "문제집 제목", example = "완전 이진 트리 개념 문제집")
        String title,

        @Schema(description = "풀이 상태", example = "COMPLETED")
        ProblemSetSolveStatus status,

        @Schema(description = "최고 점수 완료 회차의 총 문제 수 (미완료 시 null)", example = "5")
        Integer totalQuestionCount,

        @Schema(description = "최고 점수 완료 회차의 정답 수 (미완료 시 null)", example = "4")
        Integer correctCount,

        @Schema(description = "합격 여부. 전 문제 정답이면 PASS (미완료 시 null)", example = "FAIL")
        PassStatus passStatus,

        @Schema(description = "완료 횟수", example = "2")
        Integer completedCount,

        @Schema(description = "마지막 완료 시간 (미완료 시 null)")
        LocalDateTime lastCompletedAt
) {
}
