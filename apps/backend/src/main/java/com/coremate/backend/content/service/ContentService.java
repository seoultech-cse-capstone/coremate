package com.coremate.backend.content.service;

import com.coremate.backend.content.dto.response.CategoryLv1Response;
import com.coremate.backend.content.dto.response.CategoryLv2Response;
import com.coremate.backend.content.dto.response.ConceptDetailResponse;
import com.coremate.backend.content.dto.response.ConceptResponse;
import com.coremate.backend.content.dto.response.SourceRefResponse;
import com.coremate.backend.content.dto.response.SubjectResponse;
import com.coremate.backend.content.dto.type.ConceptSolveStatus;
import com.coremate.backend.content.dto.type.PassStatus;
import com.coremate.backend.content.entity.CategoryLv1;
import com.coremate.backend.content.entity.CategoryLv2;
import com.coremate.backend.content.entity.Concept;
import com.coremate.backend.content.entity.Subject;
import com.coremate.backend.content.repository.CategoryLv1Repository;
import com.coremate.backend.content.repository.CategoryLv2Repository;
import com.coremate.backend.content.repository.ConceptRepository;
import com.coremate.backend.content.repository.SubjectRepository;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.solve.dto.type.SolveSessionStatus;
import com.coremate.backend.solve.dto.type.SolveSessionType;
import com.coremate.backend.solve.entity.SolveSession;
import com.coremate.backend.solve.repository.SolveSessionRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Collections;
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
public class ContentService {

    private final SubjectRepository subjectRepository;
    private final CategoryLv1Repository categoryLv1Repository;
    private final CategoryLv2Repository categoryLv2Repository;
    private final ConceptRepository conceptRepository;
    private final SolveSessionRepository solveSessionRepository;
    private final ObjectMapper objectMapper;

    public List<SubjectResponse> getSubjects() {
        return subjectRepository.findAllByActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(s -> new SubjectResponse(
                        s.getSubjectId(),
                        s.getSubjectCode(),
                        s.getSubjectName(),
                        s.getSubjectNameEn(),
                        s.getSortOrder(),
                        s.isActive()
                ))
                .toList();
    }

    public List<CategoryLv1Response> getCategoryLv1List(Long subjectId) {
        subjectRepository.findBySubjectIdAndActiveTrue(subjectId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.SUBJECT_NOT_FOUND));
        return categoryLv1Repository.findAllBySubject_SubjectIdAndActiveTrueOrderBySortOrderAsc(subjectId)
                .stream()
                .map(c -> new CategoryLv1Response(
                        c.getCategoryLv1Id(),
                        c.getSubject().getSubjectId(),
                        c.getCategoryLv1Code(),
                        c.getCategoryLv1Name(),
                        c.getCategoryLv1NameEn(),
                        c.getDescription(),
                        c.getSortOrder(),
                        c.isActive()
                ))
                .toList();
    }

    public List<CategoryLv2Response> getCategoryLv2List(Long categoryLv1Id) {
        categoryLv1Repository.findByCategoryLv1IdAndActiveTrue(categoryLv1Id)
                .orElseThrow(() -> new CoreMateException(ErrorCode.RESOURCE_NOT_FOUND));
        return categoryLv2Repository.findAllByCategoryLv1_CategoryLv1IdAndActiveTrueOrderBySortOrderAsc(categoryLv1Id)
                .stream()
                .map(c -> new CategoryLv2Response(
                        c.getCategoryLv2Id(),
                        c.getCategoryLv1().getCategoryLv1Id(),
                        c.getCategoryLv2Code(),
                        c.getCategoryLv2Name(),
                        c.getCategoryLv2NameEn(),
                        c.getDescription(),
                        c.getSortOrder(),
                        c.isActive()
                ))
                .toList();
    }

    /**
     * 중분류 하위의 활성 개념 목록을 각 개념의 풀이 상태와 함께 반환한다.
     * 회원인 경우 본인의 개념 문제집 풀이 세션(solve_sessions)을 집계해 미풀이/진행중/완료를 계산하고,
     * 완료 기록이 있으면 최고 점수(정답률 우선) 회차 기준 총 문제 수/정답 수/합격 여부를 담는다.
     */
    public List<ConceptResponse> getConcepts(Long categoryLv2Id, Long userId) {
        categoryLv2Repository.findByCategoryLv2IdAndActiveTrue(categoryLv2Id)
                .orElseThrow(() -> new CoreMateException(ErrorCode.RESOURCE_NOT_FOUND));

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
                .map(c -> toConceptResponse(c, sessionsByConcept.getOrDefault(c.getConceptId(), List.of())))
                .toList();
    }

    private ConceptResponse toConceptResponse(Concept concept, List<SolveSession> sessions) {
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

            // 문제가 0개인 완료 회차가 합격으로 오판되지 않도록 총 문제 수 > 0 조건을 둔다.
            PassStatus passStatus = (best.getTotalQuestionCount() > 0
                    && best.getCorrectCount() == best.getTotalQuestionCount())
                    ? PassStatus.PASS : PassStatus.FAIL;

            return buildConceptResponse(concept, ConceptSolveStatus.COMPLETED,
                    best.getTotalQuestionCount(), best.getCorrectCount(), passStatus,
                    completed.size(), lastCompletedAt);
        }

        boolean inProgress = sessions.stream()
                .anyMatch(s -> s.getStatus() == SolveSessionStatus.IN_PROGRESS);
        ConceptSolveStatus status = inProgress
                ? ConceptSolveStatus.IN_PROGRESS : ConceptSolveStatus.NOT_STARTED;

        return buildConceptResponse(concept, status, null, null, null, 0, null);
    }

    private ConceptResponse buildConceptResponse(
            Concept concept, ConceptSolveStatus status,
            Integer totalQuestionCount, Integer correctCount, PassStatus passStatus,
            int completedCount, LocalDateTime lastCompletedAt) {
        return new ConceptResponse(
                concept.getConceptId(),
                concept.getConceptCode(),
                concept.getCategoryLv2().getCategoryLv2Id(),
                concept.getConceptName(),
                concept.getConceptNameEn(),
                concept.getOneLineDefinition(),
                status,
                totalQuestionCount,
                correctCount,
                passStatus,
                completedCount,
                lastCompletedAt,
                concept.getSortOrder()
        );
    }

    public ConceptDetailResponse getConceptDetail(Long conceptId) {
        Concept concept = conceptRepository.findById(conceptId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.CONCEPT_NOT_FOUND));

        CategoryLv2 lv2 = concept.getCategoryLv2();
        CategoryLv1 lv1 = lv2.getCategoryLv1();
        Subject subject = lv1.getSubject();

        return new ConceptDetailResponse(
                concept.getConceptId(),
                concept.getConceptCode(),
                subject.getSubjectId(),
                subject.getSubjectName(),
                lv1.getCategoryLv1Id(),
                lv1.getCategoryLv1Name(),
                lv2.getCategoryLv2Id(),
                lv2.getCategoryLv2Name(),
                concept.getConceptName(),
                concept.getConceptNameEn(),
                parseStringList(concept.getAliasesJson()),
                concept.getOneLineDefinition(),
                concept.getCoreSummary(),
                parseStringList(concept.getCoreKeywordsJson()),
                parseStringList(concept.getKeyPropertiesJson()),
                parseSourceRefs(concept.getSourceRefs()),
                concept.getReviewStatus(),
                concept.isActive()
        );
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new CoreMateException(ErrorCode.INVALID_CONTENT_DATA);
        }
    }

    private List<SourceRefResponse> parseSourceRefs(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new CoreMateException(ErrorCode.INVALID_CONTENT_DATA);
        }
    }
}
