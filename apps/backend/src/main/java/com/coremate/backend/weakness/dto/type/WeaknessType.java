package com.coremate.backend.weakness.dto.type;

/**
 * 약점 유형 코드. backend_candidate_type / ai_primary_type / final_type 공통 허용값.
 * (사용자 약점 분석표 수정안 기준)
 */
public enum WeaknessType {
    CONCEPT_CONFUSION,   // 개념 혼동: 개념 자체 또는 유사 개념을 혼동
    APPLICATION_GAP,     // 적용 부족: 정의는 알지만 문제 상황 적용 연결이 부족
    SPEED_FLUENCY,       // 속도·숙련도 부족: 정답 가능성은 있으나 판단·풀이 숙련도 부족
    EXECUTION_ERROR      // 풀이 실행 오류: 개념 이해보다 계산·절차·조건 확인이 불안정
}
