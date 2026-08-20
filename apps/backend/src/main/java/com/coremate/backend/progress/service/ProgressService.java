package com.coremate.backend.progress.service;

import com.coremate.backend.content.repository.ConceptRepository;
import com.coremate.backend.content.repository.SubjectRepository;
import com.coremate.backend.problemset.dto.type.ProblemSetType;
import com.coremate.backend.progress.dto.response.ConceptCountSummaryResponse;
import com.coremate.backend.progress.dto.response.ProblemSetProgressResponse;
import com.coremate.backend.progress.dto.response.SubjectProgressResponse;
import com.coremate.backend.solve.dto.type.SolveSessionType;
import com.coremate.backend.solve.entity.SolveSession;
import com.coremate.backend.solve.repository.SolveSessionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final ConceptRepository conceptRepository;
    private final SolveSessionRepository solveSessionRepository;
    private final SubjectRepository subjectRepository;

    /**
     * 복습탭 개념 카운팅(총 개념 / 풀었어요 / 아직 못풀었어요)을 전역(과목 무관) 기준으로 집계한다.
     * - 총 개념: 전체 활성 개념 수
     * - 풀었어요: 사용자가 COMPLETED 상태의 개념 문제집 세션을 가진 서로 다른 개념 수
     * - 아직 못풀었어요: 총 개념 - 풀었어요 (미풀이 + 진행중 포함)
     */
    @Transactional(readOnly = true)
    public ConceptCountSummaryResponse getConceptCountSummary(Long userId) {
        long totalConceptCount = conceptRepository.countByActiveTrue();
        long solvedConceptCount = solveSessionRepository.countDistinctSolvedConcepts(userId);
        long unsolvedConceptCount = totalConceptCount - solvedConceptCount;
        return new ConceptCountSummaryResponse(totalConceptCount, solvedConceptCount, unsolvedConceptCount);
    }

    /**
     * 과목별 학습 진행률을 조회한다(활성 과목 전체, 정렬 순서대로).
     * 홈 "Your Progress"가 쓰는 achievementRate 는 "완료한 개념 / 전체 개념" 비율(%)로 계산한다.
     * 완료 개념 기준은 복습탭 카운팅과 동일하게 COMPLETED 개념 문제집 세션이다.
     *
     * 문제 수·모의고사 완료 수·육각형 스탯 점수 필드는 아직 어떤 화면도 사용하지 않아 0으로 둔다.
     * (마이페이지 스탯 화면이 실제로 붙을 때 함께 집계한다.)
     */
    @Transactional(readOnly = true)
    public List<SubjectProgressResponse> getSubjectProgress(Long userId) {
        return subjectRepository.findAllByActiveTrueOrderBySortOrderAsc().stream()
                .map(subject -> {
                    long totalConceptCount = conceptRepository
                            .countByCategoryLv2_CategoryLv1_Subject_SubjectIdAndActiveTrue(subject.getSubjectId());
                    long completedConceptCount = solveSessionRepository
                            .countDistinctSolvedConceptsBySubject(userId, subject.getSubjectId());
                    double achievementRate = totalConceptCount > 0
                            ? BigDecimal.valueOf(completedConceptCount * 100.0 / totalConceptCount)
                                    .setScale(1, RoundingMode.HALF_UP).doubleValue()
                            : 0.0;
                    return new SubjectProgressResponse(
                            userId, subject.getSubjectId(), subject.getSubjectName(),
                            0, 0, 0,
                            (int) completedConceptCount, (int) totalConceptCount,
                            0, 0, 0, achievementRate);
                })
                .toList();
    }

    /**
     * 문제집(과목/챕터 모의고사, 개념 문제집)별 완료 상태를 조회한다.
     * 프론트는 화면 깊이별 problemSetType + 부모 필터로 호출해 targetId 로 완료 여부를 찾는다.
     * 완료(COMPLETED)된 회차가 있는 대상만 isCompleted=true 로 내려주며, 대상별로 최신 회차/완료 횟수를 함께 담는다.
     */
    @Transactional(readOnly = true)
    public List<ProblemSetProgressResponse> getProblemSetProgress(
            Long userId, ProblemSetType problemSetType,
            Long subjectId, Long categoryLv1Id, Long categoryLv2Id) {
        if (problemSetType == null) {
            // 대상 레벨(과목/대분류/중분류/개념)을 특정할 수 없어 targetId 매칭이 불가능하다.
            // 프론트는 항상 유형을 지정해 호출하므로 이 경우 빈 목록을 반환한다.
            return List.of();
        }

        List<SolveSession> sessions = problemSetType == ProblemSetType.CONCEPT_PRACTICE
                ? solveSessionRepository.findCompletedConceptPracticeSessions(userId, categoryLv2Id)
                : solveSessionRepository.findCompletedMockSessions(
                        userId, SolveSessionType.valueOf(problemSetType.name()),
                        subjectId, categoryLv1Id, categoryLv2Id);

        // targetId(유형별 대상 레벨 id) 기준으로 묶는다. 세션은 completedAt DESC 정렬이라 각 그룹 첫 항목이 최신.
        Map<Long, List<SolveSession>> byTarget = new LinkedHashMap<>();
        for (SolveSession session : sessions) {
            Long targetId = resolveTargetId(problemSetType, session);
            if (targetId == null) {
                continue;
            }
            byTarget.computeIfAbsent(targetId, key -> new ArrayList<>()).add(session);
        }

        return byTarget.entrySet().stream()
                .map(entry -> {
                    Long targetId = entry.getKey();
                    List<SolveSession> group = entry.getValue();
                    SolveSession latest = group.get(0);
                    return new ProblemSetProgressResponse(
                            null, userId, problemSetType, targetType(problemSetType), targetId,
                            latest.getSubject() != null ? latest.getSubject().getSubjectId() : null,
                            latest.getCategoryLv1() != null ? latest.getCategoryLv1().getCategoryLv1Id() : null,
                            latest.getCategoryLv2() != null ? latest.getCategoryLv2().getCategoryLv2Id() : null,
                            latest.getConcept() != null ? latest.getConcept().getConceptId() : null,
                            latest.getSolveSessionId(), true, group.size(), latest.getCompletedAt());
                })
                .toList();
    }

    private Long resolveTargetId(ProblemSetType problemSetType, SolveSession session) {
        return switch (problemSetType) {
            case SUBJECT_MOCK -> session.getSubject() != null ? session.getSubject().getSubjectId() : null;
            case CATEGORY_LV1_MOCK -> session.getCategoryLv1() != null ? session.getCategoryLv1().getCategoryLv1Id() : null;
            case CATEGORY_LV2_MOCK -> session.getCategoryLv2() != null ? session.getCategoryLv2().getCategoryLv2Id() : null;
            case CONCEPT_PRACTICE -> session.getConcept() != null ? session.getConcept().getConceptId() : null;
        };
    }

    private String targetType(ProblemSetType problemSetType) {
        return switch (problemSetType) {
            case SUBJECT_MOCK -> "SUBJECT";
            case CATEGORY_LV1_MOCK -> "CATEGORY_LV1";
            case CATEGORY_LV2_MOCK -> "CATEGORY_LV2";
            case CONCEPT_PRACTICE -> "CONCEPT";
        };
    }
}
