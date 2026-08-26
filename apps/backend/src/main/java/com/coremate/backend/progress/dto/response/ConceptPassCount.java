package com.coremate.backend.progress.dto.response;

/**
 * 범위(과목/대분류/중분류) 하나에 대한 개념 합격 집계. {@code ConceptRepository} 의 GROUP BY 쿼리 반환용이다.
 *
 * <p>문제집 탐색 화면의 진행률은 "그 범위의 활성 하위 개념 중 합격(PASS)한 개념 비율"이다
 * (FR-CONTENT-01·02·03). 비율 계산에 필요한 분모·분자를 한 번의 조회로 받는다.
 */
public record ConceptPassCount(
        Long targetId,
        long totalConceptCount,
        long passedConceptCount
) {
}
