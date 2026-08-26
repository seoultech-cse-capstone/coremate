package com.coremate.backend.progress.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.coremate.backend.content.repository.ConceptRepository;
import com.coremate.backend.progress.dto.response.ConceptPassCount;
import com.coremate.backend.solve.repository.SolveSessionRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

/**
 * 문제집 진행률과 학습 달성률이 완료(COMPLETED)가 아니라 합격(PASS)을 세는지 검증한다.
 *
 * <p>예전에는 끝까지 풀기만 하면 개념이 카운트됐다. 그래서 중분류 챕터 모의고사를 한 번 보거나
 * 개념 문제집을 다 틀리고 제출해도 문제집 화면이 완료로 보였다. FR-STAT-02·SCR-LOG-01·
 * FR-CONTENT-01~03 이 요구하는 기준은 전 문제 정답이다.
 *
 * <p>마스터 데이터 엔티티(subject·concept 등)에는 빌더가 없어 네이티브 INSERT 로 넣는다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("개념 합격(PASS) 집계")
class ConceptPassRateQueryTest {

    private static final long USER_ID = 7_700_001L;
    private static final long SUBJECT_ID = 7_710_001L;
    private static final long LV1_ID = 7_720_001L;
    private static final long LV2_ID = 7_730_001L;
    private static final long PASSED_CONCEPT_ID = 7_740_001L;
    private static final long FAILED_CONCEPT_ID = 7_740_002L;
    private static final long UNTOUCHED_CONCEPT_ID = 7_740_003L;
    private static final long INACTIVE_CONCEPT_ID = 7_740_004L;

    @Autowired private ConceptRepository conceptRepository;
    @Autowired private SolveSessionRepository solveSessionRepository;
    @Autowired private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        insertContentHierarchy();
        // 전 문제 정답 → 합격
        insertConceptPracticeSession(PASSED_CONCEPT_ID, 5, 5, "COMPLETED");
        // 끝까지 풀었지만 3/5 → 완료지만 불합격
        insertConceptPracticeSession(FAILED_CONCEPT_ID, 5, 3, "COMPLETED");
        // 비활성 개념은 다 맞혔어도 분모·분자 어디에도 들어가면 안 된다
        insertConceptPracticeSession(INACTIVE_CONCEPT_ID, 5, 5, "COMPLETED");
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("복습탭 '풀었어요'는 다 맞힌 개념만 센다")
    void solvedConceptCountUsesPassNotCompleted() {
        assertThat(solveSessionRepository.countDistinctSolvedConcepts(USER_ID)).isEqualTo(1);
    }

    @Test
    @DisplayName("과목별 학습 달성률도 다 맞힌 개념만 센다")
    void subjectConceptCountUsesPassNotCompleted() {
        assertThat(solveSessionRepository
                .countDistinctSolvedConceptsBySubject(USER_ID, SUBJECT_ID)).isEqualTo(1);
    }

    @Test
    @DisplayName("중분류 진행률은 활성 개념 3개 중 합격 1개로 집계된다")
    void categoryLv2PassCountCountsActiveConceptsOnly() {
        List<ConceptPassCount> counts =
                conceptRepository.aggregatePassCountByCategoryLv2(USER_ID, LV1_ID);

        assertThat(counts).singleElement().satisfies(count -> {
            assertThat(count.targetId()).isEqualTo(LV2_ID);
            // 비활성 개념은 빠지고 합격·불합격·미풀이 3개만 분모가 된다
            assertThat(count.totalConceptCount()).isEqualTo(3);
            assertThat(count.passedConceptCount()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("아무것도 안 푼 사용자도 분모가 있는 행으로 나온다")
    void untouchedScopeStillReturnsARowWithZeroPassed() {
        List<ConceptPassCount> counts =
                conceptRepository.aggregatePassCountByCategoryLv2(USER_ID + 1, LV1_ID);

        assertThat(counts).singleElement().satisfies(count -> {
            assertThat(count.totalConceptCount()).isEqualTo(3);
            assertThat(count.passedConceptCount()).isZero();
        });
    }

    @Test
    @DisplayName("과목·대분류 집계도 같은 기준으로 실행된다")
    void subjectAndCategoryLv1AggregationsUseTheSameRule() {
        assertThat(conceptRepository.aggregatePassCountBySubject(USER_ID))
                .filteredOn(count -> SUBJECT_ID == count.targetId())
                .singleElement()
                .satisfies(count -> {
                    assertThat(count.totalConceptCount()).isEqualTo(3);
                    assertThat(count.passedConceptCount()).isEqualTo(1);
                });

        assertThat(conceptRepository.aggregatePassCountByCategoryLv1(USER_ID, SUBJECT_ID))
                .singleElement()
                .satisfies(count -> {
                    assertThat(count.targetId()).isEqualTo(LV1_ID);
                    assertThat(count.totalConceptCount()).isEqualTo(3);
                    assertThat(count.passedConceptCount()).isEqualTo(1);
                });
    }

    private void insertContentHierarchy() {
        entityManager.createNativeQuery("""
                INSERT INTO subject (subject_id, subject_code, subject_name, sort_order, is_active,
                                     created_at, updated_at)
                VALUES (?1, 'PASSTEST', '합격집계테스트과목', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """).setParameter(1, SUBJECT_ID).executeUpdate();

        entityManager.createNativeQuery("""
                INSERT INTO category_lv1 (category_lv1_id, subject_id, category_lv1_code,
                                          category_lv1_name, sort_order, is_active, created_at, updated_at)
                VALUES (?1, ?2, 'PASSTEST_LV1', '합격집계 대분류', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """).setParameter(1, LV1_ID).setParameter(2, SUBJECT_ID).executeUpdate();

        entityManager.createNativeQuery("""
                INSERT INTO category_lv2 (category_lv2_id, category_lv1_id, category_lv2_code,
                                          category_lv2_name, sort_order, is_active, created_at, updated_at)
                VALUES (?1, ?2, 'PASSTEST_LV2', '합격집계 중분류', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """).setParameter(1, LV2_ID).setParameter(2, LV1_ID).executeUpdate();

        insertConcept(PASSED_CONCEPT_ID, "합격 개념", 1, true);
        insertConcept(FAILED_CONCEPT_ID, "불합격 개념", 2, true);
        insertConcept(UNTOUCHED_CONCEPT_ID, "미풀이 개념", 3, true);
        insertConcept(INACTIVE_CONCEPT_ID, "비활성 개념", 4, false);
    }

    private void insertConcept(long conceptId, String name, int sortOrder, boolean active) {
        entityManager.createNativeQuery("""
                INSERT INTO concept (concept_id, category_lv2_id, concept_code, concept_name,
                                     one_line_definition, core_summary, core_keywords_json,
                                     key_properties_json, sort_order, review_status, is_active,
                                     version_no, created_at, updated_at)
                VALUES (?1, ?2, ?3, ?4, '정의', '요약', '[]', '[]', ?5, 'APPROVED', ?6, 1,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """)
                .setParameter(1, conceptId)
                .setParameter(2, LV2_ID)
                .setParameter(3, "PASSTEST_C" + conceptId)
                .setParameter(4, name)
                .setParameter(5, sortOrder)
                .setParameter(6, active)
                .executeUpdate();
    }

    private void insertConceptPracticeSession(
            long conceptId, int totalQuestionCount, int correctCount, String status) {
        entityManager.createNativeQuery("""
                INSERT INTO solve_sessions (user_id, session_type, concept_id, title,
                                            total_question_count, correct_count, wrong_count,
                                            total_time_spent_sec, status, started_at, completed_at,
                                            created_at, updated_at)
                VALUES (?1, 'CONCEPT_PRACTICE', ?2, '합격집계 회차', ?3, ?4, ?5, 60, ?6,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """)
                .setParameter(1, USER_ID)
                .setParameter(2, conceptId)
                .setParameter(3, totalQuestionCount)
                .setParameter(4, correctCount)
                .setParameter(5, totalQuestionCount - correctCount)
                .setParameter(6, status)
                .executeUpdate();
    }
}
