package com.coremate.backend.weakness.dto.type;

/**
 * 약점 근거 유형. MVP 핵심 3종(WRONG/LONG_TIME/HINT_USED),
 * ANSWER_CHANGED는 기록·해설 화면 및 추후 분석 고도화용 보조 근거.
 */
public enum EvidenceType {
    WRONG,           // 틀린 문제
    LONG_TIME,       // 맞았지만 기준보다 오래 걸린 문제
    HINT_USED,       // 힌트·물음표를 많이 사용한 문제
    ANSWER_CHANGED   // 답 변경(보조/추후)
}
