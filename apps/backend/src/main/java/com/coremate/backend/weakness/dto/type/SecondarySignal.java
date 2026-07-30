package com.coremate.backend.weakness.dto.type;

/**
 * 보조 신호 코드. 주 약점 유형만으로 설명되지 않는 함께 관찰된 패턴(개념당 최대 2개).
 */
public enum SecondarySignal {
    REPEATED_MISCONCEPTION,      // 동일 오개념 반복
    CONCEPT_RELATION_CONFUSION,  // 개념 관계 혼동
    HINT_DEPENDENCY,             // 힌트 의존 신호
    SLOW_FLUENCY,                // 느린 풀이 신호
    INCONSISTENT_EXECUTION       // 불안정한 풀이 수행
}
