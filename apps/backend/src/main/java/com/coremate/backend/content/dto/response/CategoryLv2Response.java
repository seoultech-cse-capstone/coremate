package com.coremate.backend.content.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "중분류 목록 응답 DTO")
public record CategoryLv2Response(
        @Schema(description = "중분류 ID", example = "101")
        Long categoryLv2Id,

        @Schema(description = "대분류 ID", example = "10")
        Long categoryLv1Id,

        @Schema(description = "중분류 코드", example = "DS_TREE_BINARY_TREE")
        String categoryLv2Code,

        @Schema(description = "중분류명", example = "이진 트리")
        String categoryLv2Name,

        @Schema(description = "중분류 영문명", example = "Binary Tree")
        String categoryLv2NameEn,

        @Schema(description = "중분류 설명", example = "각 노드가 최대 두 개의 자식을 갖는 트리 구조")
        String description,

        @Schema(description = "노출 순서", example = "1")
        Integer sortOrder,

        @Schema(description = "사용 여부", example = "true")
        Boolean isActive
) {
}
