package com.coremate.backend.weakness.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "약점 개념과 이어진 개념. concept_relation 에서 온다")
public record RelatedConceptResponse(
        @Schema(description = "개념 ID", example = "9552")
        Long conceptId,

        @Schema(description = "개념명", example = "빅오 표기법")
        String conceptName,

        @Schema(description = "관계 유형. PREREQUISITE(먼저 볼 개념) 또는 CONFUSING(헷갈리는 개념)",
                example = "PREREQUISITE")
        String relationType,

        @Schema(description = "관계 메모. 왜 이어지는지 콘텐츠 검수 단계에서 적어 둔 문장",
                example = "빅세타는 상한 개념인 빅오 이해를 바탕으로 더 잘 이해된다.")
        String relationNote,

        @Schema(description = "이 개념이 같은 분석표에서도 약점이면 그 순위. 아니면 null", example = "1")
        Integer weaknessRank
) {
}
