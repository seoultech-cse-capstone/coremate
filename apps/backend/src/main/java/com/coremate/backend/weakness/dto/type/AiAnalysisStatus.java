package com.coremate.backend.weakness.dto.type;

/**
 * AI 분석 판정 상태. 개념별 해석에 대한 AI의 자기 판정.
 */
public enum AiAnalysisStatus {
    SUPPORTED,     // 근거 충분: 대표 근거가 일관되게 하나의 주 유형을 뒷받침
    AMBIGUOUS,     // 판단 모호: 둘 이상 유형이 비슷하게 가능하거나 근거 충돌
    INSUFFICIENT   // 근거 불충분: 제공 근거만으로 유형·원인 해석 불가
}
