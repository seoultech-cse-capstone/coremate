package com.coremate.backend.progress.dto.type;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 문제집 탐색 화면에서 진행률을 매기는 범위.
 *
 * <p>하위 개념은 여기 없다. 개념의 진행률은 최고 점수 회차의 정답률이라 산식이 다르고,
 * 이미 개념 목록 API(FR-CONTENT-04)가 총 문제 수·정답 수를 함께 내려주기 때문이다.
 */
@Schema(description = "진행률 집계 범위")
public enum ProgressScopeType {
    @Schema(description = "과목 기준. 활성 과목 전체를 내려준다")
    SUBJECT,

    @Schema(description = "대분류 기준. parentId 는 과목 ID")
    CATEGORY_LV1,

    @Schema(description = "중분류 기준. parentId 는 대분류 ID")
    CATEGORY_LV2
}
