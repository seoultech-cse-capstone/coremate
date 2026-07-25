package com.coremate.backend.weakness.dto.type;

/**
 * 진단 프로필 검수 상태. 승인(APPROVED)된 프로필만 AI 입력·사용자 화면에 사용한다.
 */
public enum DiagnosticReviewStatus {
    DRAFT,     // 초안(AI 생성 직후)
    APPROVED,  // 검수 승인
    REJECTED,  // 반려
    ARCHIVED   // 보관(비활성)
}
