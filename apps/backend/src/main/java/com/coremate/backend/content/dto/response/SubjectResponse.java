package com.coremate.backend.content.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "과목 목록 응답 DTO")
public record SubjectResponse(
        @Schema(description = "과목 ID", example = "1")
        Long subjectId,

        @Schema(description = "과목 코드", example = "DS")
        String subjectCode,

        @Schema(description = "과목명", example = "자료구조")
        String subjectName,

        @Schema(description = "과목 영문명", example = "Data Structures")
        String subjectNameEn,

        @Schema(description = "노출 순서", example = "1")
        Integer sortOrder,

        @Schema(description = "사용 여부", example = "true")
        Boolean isActive
) {
}
