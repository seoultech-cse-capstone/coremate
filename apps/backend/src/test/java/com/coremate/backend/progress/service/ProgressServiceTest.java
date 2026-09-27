package com.coremate.backend.progress.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.coremate.backend.content.entity.Subject;
import com.coremate.backend.content.repository.ConceptRepository;
import com.coremate.backend.content.repository.SubjectRepository;
import com.coremate.backend.problemset.dto.type.ProblemSetType;
import com.coremate.backend.progress.dto.response.ConceptCountSummaryResponse;
import com.coremate.backend.progress.dto.response.ProblemSetProgressResponse;
import com.coremate.backend.progress.dto.response.SubjectProgressResponse;
import com.coremate.backend.solve.dto.response.SubjectQuestionCount;
import com.coremate.backend.solve.dto.type.SolveSessionType;
import com.coremate.backend.solve.entity.SolveSession;
import com.coremate.backend.solve.repository.SolveLogRepository;
import com.coremate.backend.solve.repository.SolveSessionRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock
    private ConceptRepository conceptRepository;

    @Mock
    private SolveLogRepository solveLogRepository;

    @Mock
    private SolveSessionRepository solveSessionRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private ProgressService progressService;

    @Test
    void 개념_카운팅은_전체에서_풀었어요를_뺀_값을_아직_못풀었어요로_계산한다() {
        long userId = 1L;
        when(conceptRepository.countByActiveTrue()).thenReturn(500L);
        when(solveSessionRepository.countDistinctSolvedConcepts(userId)).thenReturn(100L);

        ConceptCountSummaryResponse result = progressService.getConceptCountSummary(userId);

        assertThat(result.totalConceptCount()).isEqualTo(500L);
        assertThat(result.solvedConceptCount()).isEqualTo(100L);
        assertThat(result.unsolvedConceptCount()).isEqualTo(400L);
    }

    @Test
    void 풀이한_개념이_없으면_전체가_아직_못풀었어요가_된다() {
        long userId = 2L;
        when(conceptRepository.countByActiveTrue()).thenReturn(42L);
        when(solveSessionRepository.countDistinctSolvedConcepts(userId)).thenReturn(0L);

        ConceptCountSummaryResponse result = progressService.getConceptCountSummary(userId);

        assertThat(result.totalConceptCount()).isEqualTo(42L);
        assertThat(result.solvedConceptCount()).isEqualTo(0L);
        assertThat(result.unsolvedConceptCount()).isEqualTo(42L);
    }

    @Test
    void 과목_달성률은_완료한_개념을_전체_개념으로_나눈_비율이다() {
        long userId = 1L;
        Subject subject = org.mockito.Mockito.mock(Subject.class);
        when(subject.getSubjectId()).thenReturn(10L);
        when(subject.getSubjectName()).thenReturn("자료구조");
        when(subjectRepository.findAllByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(subject));
        when(conceptRepository.countByCategoryLv2_CategoryLv1_Subject_SubjectIdAndActiveTrue(10L))
                .thenReturn(40L);
        when(solveSessionRepository.countDistinctSolvedConceptsBySubject(userId, 10L)).thenReturn(12L);
        when(solveLogRepository.aggregateQuestionCountsBySubject(userId)).thenReturn(List.of());

        List<SubjectProgressResponse> result = progressService.getSubjectProgress(userId);

        assertThat(result).hasSize(1);
        SubjectProgressResponse subjectProgress = result.get(0);
        assertThat(subjectProgress.subjectId()).isEqualTo(10L);
        assertThat(subjectProgress.subjectName()).isEqualTo("자료구조");
        assertThat(subjectProgress.completedConceptCount()).isEqualTo(12);
        assertThat(subjectProgress.totalConceptCount()).isEqualTo(40);
        assertThat(subjectProgress.achievementRate()).isEqualTo(30.0);
        // 집계 행이 없는 과목(풀이 기록 없음)은 문항 카운트가 0이어야 한다.
        assertThat(subjectProgress.solvedQuestionCount()).isZero();
        assertThat(subjectProgress.correctQuestionCount()).isZero();
        assertThat(subjectProgress.wrongQuestionCount()).isZero();
    }

    @Test
    void 과목별_문항_카운트는_solve_logs_집계에서_채우고_오답은_차이로_계산한다() {
        long userId = 1L;
        Subject subject = org.mockito.Mockito.mock(Subject.class);
        when(subject.getSubjectId()).thenReturn(10L);
        when(subject.getSubjectName()).thenReturn("자료구조");
        when(subjectRepository.findAllByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(subject));
        when(conceptRepository.countByCategoryLv2_CategoryLv1_Subject_SubjectIdAndActiveTrue(10L))
                .thenReturn(40L);
        when(solveSessionRepository.countDistinctSolvedConceptsBySubject(userId, 10L)).thenReturn(12L);
        when(solveLogRepository.aggregateQuestionCountsBySubject(userId))
                .thenReturn(List.of(new SubjectQuestionCount(10L, 50L, 38L)));

        SubjectProgressResponse subjectProgress = progressService.getSubjectProgress(userId).get(0);

        assertThat(subjectProgress.solvedQuestionCount()).isEqualTo(50);
        assertThat(subjectProgress.correctQuestionCount()).isEqualTo(38);
        assertThat(subjectProgress.wrongQuestionCount()).isEqualTo(12);
    }

    @Test
    void 전체_개념이_0이면_달성률은_0이다() {
        long userId = 1L;
        Subject subject = org.mockito.Mockito.mock(Subject.class);
        when(subject.getSubjectId()).thenReturn(10L);
        when(subject.getSubjectName()).thenReturn("빈과목");
        when(subjectRepository.findAllByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(subject));
        when(conceptRepository.countByCategoryLv2_CategoryLv1_Subject_SubjectIdAndActiveTrue(10L))
                .thenReturn(0L);
        when(solveSessionRepository.countDistinctSolvedConceptsBySubject(userId, 10L)).thenReturn(0L);
        when(solveLogRepository.aggregateQuestionCountsBySubject(userId)).thenReturn(List.of());

        List<SubjectProgressResponse> result = progressService.getSubjectProgress(userId);

        assertThat(result.get(0).achievementRate()).isEqualTo(0.0);
    }

    @Test
    void 문제집_유형이_없으면_빈_목록을_반환한다() {
        List<ProblemSetProgressResponse> result =
                progressService.getProblemSetProgress(1L, null, null, null, null);

        assertThat(result).isEmpty();
    }

    @Test
    void 문제집_완료_상태는_대상별로_최신회차와_완료횟수를_담는다() {
        long userId = 1L;
        Subject subject = org.mockito.Mockito.mock(Subject.class);
        lenient().when(subject.getSubjectId()).thenReturn(5L);

        // 같은 과목(targetId=5)에 대한 완료 회차 2건(completedAt DESC 정렬 상태로 전달됨)
        SolveSession latest = org.mockito.Mockito.mock(SolveSession.class);
        when(latest.getSubject()).thenReturn(subject);
        when(latest.getSolveSessionId()).thenReturn(9002L);
        SolveSession older = org.mockito.Mockito.mock(SolveSession.class);
        when(older.getSubject()).thenReturn(subject);

        when(solveSessionRepository.findCompletedMockSessions(
                userId, SolveSessionType.SUBJECT_MOCK, null, null, null))
                .thenReturn(List.of(latest, older));

        List<ProblemSetProgressResponse> result = progressService.getProblemSetProgress(
                userId, ProblemSetType.SUBJECT_MOCK, null, null, null);

        assertThat(result).hasSize(1);
        ProblemSetProgressResponse row = result.get(0);
        assertThat(row.targetId()).isEqualTo(5L);
        assertThat(row.targetType()).isEqualTo("SUBJECT");
        assertThat(row.isCompleted()).isTrue();
        assertThat(row.completedCount()).isEqualTo(2);
        assertThat(row.lastSolveSessionId()).isEqualTo(9002L);
    }
}
