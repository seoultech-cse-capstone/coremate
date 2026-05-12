package com.coremate.backend.content.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "하위 개념 상세 응답 DTO")
public record ConceptDetailResponse(
        @Schema(description = "개념 ID", example = "1001")
        Long conceptId,

        @Schema(description = "개념 코드", example = "DS_TREE_COMPLETE_BINARY_TREE")
        String conceptCode,

        @Schema(description = "과목 ID", example = "1")
        Long subjectId,

        @Schema(description = "과목명", example = "자료구조")
        String subjectName,

        @Schema(description = "대분류 ID", example = "20")
        Long categoryLv1Id,

        @Schema(description = "대분류명", example = "트리")
        String categoryLv1Name,

        @Schema(description = "중분류 ID", example = "101")
        Long categoryLv2Id,

        @Schema(description = "중분류명", example = "이진 트리")
        String categoryLv2Name,

        @Schema(description = "개념명", example = "완전 이진 트리")
        String conceptName,

        @Schema(description = "개념 영문명", example = "Complete Binary Tree")
        String conceptNameEn,

        @Schema(description = "표기 변형 및 동의어 목록", example = "[\"완전이진트리\", \"Complete Binary Tree\"]")
        List<String> aliases,

        @Schema(description = "한 줄 정의")
        String oneLineDefinition,

        @Schema(description = "핵심 요약")
        String coreSummary,

        @Schema(description = "핵심 키워드")
        List<String> coreKeywords,

        @Schema(description = "핵심 성질")
        List<String> keyProperties,

        @Schema(description = "출처 메타데이터 목록")
        List<SourceRefResponse> sourceRefs,

        @Schema(description = "검수 상태", example = "approved")
        String reviewStatus,

        @Schema(description = "서비스 사용 여부", example = "true")
        Boolean isActive
) {
}