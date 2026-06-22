package com.coremate.backend.content.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대분류 목록 응답 DTO")
public record CategoryLv1Response(
        @Schema(description = "대분류 ID", example = "10")
        Long categoryLv1Id,

        @Schema(description = "과목 ID", example = "1")
        Long subjectId,

        @Schema(description = "대분류 코드", example = "DS_TREE")
        String categoryLv1Code,

        @Schema(description = "대분류명", example = "트리")
        String categoryLv1Name,

        @Schema(description = "대분류 영문명", example = "Tree")
        String categoryLv1NameEn,

        @Schema(description = "대분류 설명", example = "계층 구조를 표현하는 비선형 자료구조")
        String description,

        @Schema(description = "노출 순서", example = "3")
        Integer sortOrder,

        @Schema(description = "사용 여부", example = "true")
        Boolean isActive
) {
}
