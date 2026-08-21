package com.coremate.backend.solve.dto.response;

/**
 * 과목별 문항 풀이 집계 결과. {@code SolveLogRepository} 의 GROUP BY 쿼리 반환용이다.
 *
 * <p>과목별 스탯(FR-STAT-01)이 쓰는 "푼 문제 수 / 맞힌 문제 수"를 한 번의 조회로 받는다.
 * 오답 수는 두 값의 차이라 따로 담지 않는다.
 */
public record SubjectQuestionCount(
        Long subjectId,
        long solvedCount,
        long correctCount
) {
}
