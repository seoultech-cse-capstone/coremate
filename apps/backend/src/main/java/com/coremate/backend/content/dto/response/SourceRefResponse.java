package com.coremate.backend.content.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "개념 출처 메타데이터 응답 DTO")
public record SourceRefResponse(

        @Schema(description = "PDF chunk ID", example = "DATA_STRUCTURE_open_data_structures_p120")
        String chunkId,

        @Schema(description = "출처 파일명", example = "open_data_structures.pdf")
        String sourceFile,

        @Schema(description = "출처 페이지", example = "120")
        Integer page
) {
}