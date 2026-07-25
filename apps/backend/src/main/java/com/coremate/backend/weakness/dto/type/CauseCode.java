package com.coremate.backend.weakness.dto.type;

/**
 * 예상 원인 코드. AI가 근거에 기반해 추론한 약점의 예상 원인.
 */
public enum CauseCode {
    TERM_CONFUSION,            // 유사 용어 혼동
    RELATIONSHIP_MAPPING_GAP,  // 조건·개념 간 관계 연결 부족
    APPLICATION_GAP,           // 정의를 문제 상황에 적용하는 능력 부족
    PROCEDURE_FLUENCY_GAP,     // 풀이 절차 숙련도 부족
    REPEATED_MISCONCEPTION,    // 동일 오개념 반복
    CARELESS_EXECUTION         // 개념 이해보다 풀이 과정의 반복 실수 가능성
}
