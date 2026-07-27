package com.coremate.backend.problemset.service;

import com.coremate.backend.content.entity.Concept;
import com.coremate.backend.content.repository.CategoryLv2Repository;
import com.coremate.backend.content.repository.ConceptRepository;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.problemset.dto.response.ProblemSetSummaryResponse;
import com.coremate.backend.problemset.dto.type.PassStatus;
import com.coremate.backend.problemset.dto.type.ProblemSetSolveStatus;
import com.coremate.backend.problemset.dto.type.ProblemSetType;
import com.coremate.backend.solve.dto.type.SolveSessionStatus;
import com.coremate.backend.solve.dto.type.SolveSessionType;
import com.coremate.backend.solve.entity.SolveSession;
import com.coremate.backend.solve.repository.SolveSessionRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemSetService {

    private final CategoryLv2Repository categoryLv2Repository;
    private final ConceptRepository conceptRepository;
    private final SolveSessionRepository solveSessionRepository;

    public List<ProblemSetSummaryResponse> getProblemSetSummaries(
            ProblemSetType problemSetType, Long parentId, Long userId) {

        if (problemSetType != ProblemSetType.CONCEPT_PRACTICE) {
            // 모의고사 유형은 "생성되는 회차"라 목록화 대상이 아니다. 개념 문제집만 지원한다.
            throw new CoreMateException(ErrorCode.UNSUPPORTED_PROBLEM_SET_TYPE);
        }
        return getConceptProblemSetSummaries(parentId, userId);
    }

    /**
     * 중분류(categoryLv2) 하위의 개념 문제집 목록을 각 개념의 풀이 상태와 함께 반환한다.
     * 완료 기록이 있으면 최고 점수(정답률 우선) 회차 기준으로 총 문제 수/정답 수/합격 여부를 담는다.
     */
    private List<ProblemSetSummaryResponse> getConceptProblemSetSummaries(Long categoryLv2Id, Long userId) {
        categoryLv2Repository.findByCategoryLv2IdAndActiveTrue(categoryLv2Id)
                .orElseThrow(() -> new CoreMateException(ErrorCode.CATEGORY_LV2_NOT_FOUND));

        List<Concept> concepts =
                conceptRepository.findAllByCategoryLv2_CategoryLv2IdAndActiveTrueOrderBySortOrderAsc(categoryLv2Id);
        List<Long> conceptIds = concepts.stream().map(Concept::getConceptId).toList();

        // 회원인 경우에만 개념별 세션을 조회해 상태를 계산한다. (비로그인은 전부 NOT_STARTED)
        Map<Long, List<SolveSession>> sessionsByConcept = (userId != null && !conceptIds.isEmpty())
                ? solveSessionRepository
                        .findByUserIdAndSessionTypeAndConcept_ConceptIdIn(
                                userId, SolveSessionType.CONCEPT_PRACTICE, conceptIds)
                        .stream()
                        .collect(Collectors.groupingBy(s -> s.getConcept().getConceptId()))
                : Map.of();

        return concepts.stream()
                .map(concept -> toSummary(concept, sessionsByConcept.getOrDefault(concept.getConceptId(), List.of())))
                .toList();
    }

    private ProblemSetSummaryResponse toSummary(Concept concept, List<SolveSession> sessions) {
        List<SolveSession> completed = sessions.stream()
                .filter(s -> s.getStatus() == SolveSessionStatus.COMPLETED)
                .toList();

        if (!completed.isEmpty()) {
            // 최고 점수 회차: 정답률 내림차순 → 완료 시각 내림차순
            SolveSession best = completed.stream()
                    .max(Comparator
                            .comparing(SolveSession::getCorrectRate,
                                    Comparator.nullsFirst(Comparator.naturalOrder()))
                            .thenComparing(SolveSession::getCompletedAt,
                                    Comparator.nullsFirst(Comparator.naturalOrder())))
                    .orElseThrow();

            LocalDateTime lastCompletedAt = completed.stream()
                    .map(SolveSession::getCompletedAt)
                    .filter(t -> t != null)
                    .max(Comparator.naturalOrder())
                    .orElse(null);

            PassStatus passStatus = best.getCorrectCount() == best.getTotalQuestionCount()
                    ? PassStatus.PASS : PassStatus.FAIL;

            return buildResponse(concept, ProblemSetSolveStatus.COMPLETED,
                    best.getTotalQuestionCount(), best.getCorrectCount(), passStatus,
                    completed.size(), lastCompletedAt);
        }

        boolean inProgress = sessions.stream()
                .anyMatch(s -> s.getStatus() == SolveSessionStatus.IN_PROGRESS);
        ProblemSetSolveStatus status = inProgress
                ? ProblemSetSolveStatus.IN_PROGRESS : ProblemSetSolveStatus.NOT_STARTED;

        return buildResponse(concept, status, null, null, null, 0, null);
    }

    private ProblemSetSummaryResponse buildResponse(
            Concept concept, ProblemSetSolveStatus status,
            Integer totalQuestionCount, Integer correctCount, PassStatus passStatus,
            int completedCount, LocalDateTime lastCompletedAt) {
        return new ProblemSetSummaryResponse(
                ProblemSetType.CONCEPT_PRACTICE,
                "CONCEPT",
                concept.getConceptId(),
                concept.getConceptName() + " 개념 문제집",
                status,
                totalQuestionCount,
                correctCount,
                passStatus,
                completedCount,
                lastCompletedAt
        );
    }
}
