package com.coremate.backend.progress.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.coremate.backend.content.repository.ConceptRepository;
import com.coremate.backend.solve.dto.type.SolveSessionType;
import com.coremate.backend.solve.repository.SolveLogRepository;
import com.coremate.backend.solve.repository.SolveSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

/**
 * 진행률 도메인에서 새로 추가한 조회 쿼리들이 H2(MySQL 모드)에서 실제로 파싱·실행되는지 검증한다.
 * 목킹 유닛 테스트(ProgressServiceTest)는 집계 로직만 보고 JPQL 경로식/조인 유효성은 못 잡으므로,
 * 데이터가 없는 상태(빈 결과)라도 쿼리를 실행해 매핑 오류를 조기에 드러낸다.
 *
 * JPA 슬라이스(@DataJpaTest)만 띄운다. 전체 컨텍스트(@SpringBootTest)는 별도의 프로퍼티 설정에
 * 의존해 이 쿼리 검증에는 불필요하다. replace=NONE 으로 테스트 application.yaml 의
 * H2(MODE=MySQL) 설정을 그대로 쓴다(엔티티의 MySQL 전용 컬럼 정의와 호환되는 구성).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProgressQueryExecutionTest {

    private static final Long ABSENT_USER_ID = 9_900_001L;
    private static final Long ABSENT_ID = 9_900_002L;

    @Autowired
    private ConceptRepository conceptRepository;

    @Autowired
    private SolveLogRepository solveLogRepository;

    @Autowired
    private SolveSessionRepository solveSessionRepository;

    @Test
    void 과목별_전체개념_카운트_쿼리가_실행된다() {
        assertThat(conceptRepository
                .countByCategoryLv2_CategoryLv1_Subject_SubjectIdAndActiveTrue(ABSENT_ID))
                .isZero();
    }

    @Test
    void 과목별_완료개념_카운트_쿼리가_실행된다() {
        assertThat(solveSessionRepository
                .countDistinctSolvedConceptsBySubject(ABSENT_USER_ID, ABSENT_ID))
                .isZero();
    }

    @Test
    void 모의고사_완료회차_조회_쿼리가_필터별로_실행된다() {
        assertThat(solveSessionRepository.findCompletedMockSessions(
                ABSENT_USER_ID, SolveSessionType.SUBJECT_MOCK, null, null, null)).isEmpty();
        assertThat(solveSessionRepository.findCompletedMockSessions(
                ABSENT_USER_ID, SolveSessionType.CATEGORY_LV1_MOCK, ABSENT_ID, null, null)).isEmpty();
        assertThat(solveSessionRepository.findCompletedMockSessions(
                ABSENT_USER_ID, SolveSessionType.CATEGORY_LV2_MOCK, null, ABSENT_ID, null)).isEmpty();
    }

    /**
     * 과목별 문항 카운트는 생성자 표현식 + 5단계 경로식(question -> concept -> categoryLv2 ->
     * categoryLv1 -> subject) + CASE WHEN 집계라, 목킹 유닛 테스트로는 JPQL 유효성을 못 잡는다.
     */
    @Test
    void 과목별_문항카운트_집계_쿼리가_실행된다() {
        assertThat(solveLogRepository.aggregateQuestionCountsBySubject(ABSENT_USER_ID)).isEmpty();
    }

    @Test
    void 개념문제집_완료회차_조회_쿼리가_실행된다() {
        assertThat(solveSessionRepository
                .findCompletedConceptPracticeSessions(ABSENT_USER_ID, null)).isEmpty();
        assertThat(solveSessionRepository
                .findCompletedConceptPracticeSessions(ABSENT_USER_ID, ABSENT_ID)).isEmpty();
    }
}
