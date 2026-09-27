package com.coremate.backend.content.dto.response;

import com.coremate.backend.content.dto.type.ConceptSolveStatus;
import com.coremate.backend.content.dto.type.PassStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "하위 개념 목록 응답 DTO. 회원은 개념별 풀이 상태(미풀이/진행중/완료)와 "
        + "완료 시 최고 점수 회차 기준 총 문제 수/정답 수/합격 여부를 함께 반환한다.")
public record ConceptResponse(
        @Schema(description = "개념 ID", example = "1001")
        Long conceptId,

        @Schema(description = "개념 코드", example = "DS_TREE_COMPLETE_BINARY_TREE")
        String conceptCode,

        @Schema(description = "중분류 ID", example = "101")
        Long categoryLv2Id,

        @Schema(description = "개념명", example = "완전 이진 트리")
        String conceptName,

        @Schema(description = "개념 영문명", example = "Complete Binary Tree")
        String conceptNameEn,

        @Schema(description = "한 줄 정의", example = "마지막 레벨을 제외한 모든 레벨이 채워지고, 마지막 레벨은 왼쪽부터 채워지는 이진 트리")
        String oneLineDefinition,

        @Schema(description = "풀이 상태. 비로그인 사용자는 NOT_STARTED", example = "COMPLETED")
        ConceptSolveStatus status,

        @Schema(description = "최고 점수 완료 회차의 총 문제 수 (미완료 시 null)", example = "5")
        Integer totalQuestionCount,

        @Schema(description = "최고 점수 완료 회차의 정답 수 (미완료 시 null)", example = "4")
        Integer correctCount,

        @Schema(description = "합격 여부. 전 문제 정답이면 PASS (미완료 시 null)", example = "FAIL")
        PassStatus passStatus,

        @Schema(description = "완료 횟수", example = "2")
        Integer completedCount,

        @Schema(description = "마지막 완료 시간 (미완료 시 null)")
        LocalDateTime lastCompletedAt,

        @Schema(description = "노출 순서", example = "2")
        Integer sortOrder
) {
}
