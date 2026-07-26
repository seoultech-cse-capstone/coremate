package com.coremate.backend.solve.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "문제 힌트 응답 DTO")
public record QuestionHintResponse(
        @Schema(description = "문제 ID", example = "5001")
        Long questionId,

        @Schema(description = "힌트로 연결된 공통 오해 ID", example = "3001")
        Long hintCommonMistakeId,

        @Schema(description = "힌트 내용. common_mistake.correction_text 기반", example = "완전 이진 트리는 모든 레벨이 꽉 차야 하는 것이 아니라 마지막 레벨은 왼쪽부터 연속적으로 채워지면 됩니다.")
        String hintText
) {
}
