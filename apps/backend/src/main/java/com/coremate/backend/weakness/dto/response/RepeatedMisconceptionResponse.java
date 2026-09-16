package com.coremate.backend.weakness.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "한 개념에서 두 번 이상 반복된 오개념")
public record RepeatedMisconceptionResponse(
        @Schema(description = "오개념 코드", example = "ALG-C-01-01-01_M02")
        String code,

        @Schema(description = "오개념 제목. 코드에 맞는 common_mistake 가 없으면 null",
                example = "의사코드와 알고리즘을 같은 개념으로 봄")
        String title,

        @Schema(description = "반복 횟수", example = "2")
        int count
) {
}
