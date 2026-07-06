package com.coremate.backend.content.service;

import com.coremate.backend.content.dto.response.CategoryLv1Response;
import com.coremate.backend.content.dto.response.CategoryLv2Response;
import com.coremate.backend.content.dto.response.ConceptDetailResponse;
import com.coremate.backend.content.dto.response.ConceptResponse;
import com.coremate.backend.content.dto.response.SourceRefResponse;
import com.coremate.backend.content.dto.response.SubjectResponse;
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
import com.coremate.backend.progress.repository.UserConceptProgressRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
    private final UserConceptProgressRepository userConceptProgressRepository;
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

    public List<ConceptResponse> getConcepts(Long categoryLv2Id, Long userId) {
        categoryLv2Repository.findByCategoryLv2IdAndActiveTrue(categoryLv2Id)
                .orElseThrow(() -> new CoreMateException(ErrorCode.RESOURCE_NOT_FOUND));

        List<com.coremate.backend.content.entity.Concept> concepts =
                conceptRepository.findAllByCategoryLv2_CategoryLv2IdAndActiveTrueOrderBySortOrderAsc(categoryLv2Id);

        List<Long> conceptIds = concepts.stream().map(Concept::getConceptId).toList();
        Set<Long> completedIds = userId != null
                ? userConceptProgressRepository.findCompletedConceptIds(userId, conceptIds)
                : Set.of();

        return concepts.stream()
                .map(c -> new ConceptResponse(
                        c.getConceptId(),
                        c.getConceptCode(),
                        c.getCategoryLv2().getCategoryLv2Id(),
                        c.getConceptName(),
                        c.getConceptNameEn(),
                        c.getOneLineDefinition(),
                        completedIds.contains(c.getConceptId()),
                        c.getSortOrder()
                ))
                .toList();
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
