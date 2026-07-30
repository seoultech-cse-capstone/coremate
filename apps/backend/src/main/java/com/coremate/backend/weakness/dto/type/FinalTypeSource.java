package com.coremate.backend.weakness.dto.type;

/**
 * 최종 유형 결정 근거. 백엔드 교차 검증 결과 final_type을 무엇으로 확정했는지.
 */
public enum FinalTypeSource {
    BOTH,  // 백엔드 유형 후보와 AI 주 유형이 일치
    AI,    // 정량 차이가 작고 근거 호환되어 AI 판단 채택
    NONE   // 최종 유형 미확정(AMBIGUOUS/INSUFFICIENT)
}
