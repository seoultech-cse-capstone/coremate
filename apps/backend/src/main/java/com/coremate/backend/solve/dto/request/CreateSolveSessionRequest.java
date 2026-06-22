package com.coremate.backend.solve.dto.request;

import com.coremate.backend.solve.dto.type.SolveSessionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "풀이 세션 생성 요청 DTO")
public record CreateSolveSessionRequest(
        @NotNull
        @Schema(description = "풀이 세션 유형", example = "CONCEPT_PRACTICE", requiredMode = Schema.RequiredMode.REQUIRED)
        SolveSessionType sessionType,

        @NotNull
        @Schema(description = "풀이 대상 ID. sessionType에 따라 concept/category/subject ID", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
        Long targetId,

        @Schema(description = "출제할 문제 ID 목록. 비우면 백엔드가 대상 범위에서 자동 구성", example = "[5001, 5002, 5003]")
        List<Long> questionIds
) {
}
