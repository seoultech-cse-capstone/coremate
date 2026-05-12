package com.coremate.backend.content.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "하위 개념 목록 응답 DTO")
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

        @Schema(description = "회원 기준 완료 여부. 비로그인 사용자는 false 또는 null 처리 가능", example = "true")
        Boolean isCompleted,

        @Schema(description = "노출 순서", example = "2")
        Integer sortOrder
) {
}
