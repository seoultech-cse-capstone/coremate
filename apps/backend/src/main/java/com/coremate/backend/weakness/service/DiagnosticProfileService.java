package com.coremate.backend.weakness.service;

import com.coremate.backend.weakness.config.WeaknessAnalysisProperties;
import com.coremate.backend.weakness.dto.response.ConceptLearningResponse;
import com.coremate.backend.weakness.dto.type.DiagnosticReviewStatus;
import com.coremate.backend.weakness.entity.ConceptDiagnosticProfile;
import com.coremate.backend.weakness.entity.QuestionDiagnosticProfile;
import com.coremate.backend.weakness.repository.ConceptDiagnosticProfileRepository;
import com.coremate.backend.weakness.repository.QuestionDiagnosticProfileRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * 콘텐츠 등록 단계에서 미리 생성해 둔 진단 프로필을 읽어 약점분석에 결합한다.
 * 승인(APPROVED)되고 설정된 버전과 일치하는 프로필만 사용한다.
 *
 * <p>{@code weakness.analysis.diagnostic-profile-version} 이 {@code none} 이면 전부 비활성이며,
 * 프로필이 없는 개념/문항은 조용히 건너뛴다(프로필 없이도 기존과 동일하게 동작).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiagnosticProfileService {

    private static final String DISABLED = "none";

    /**
     * 오개념 코드로 세지 않는 표시값(파이프라인 schemas/question_diagnostic_profile_schema.py 와 일치).
     * NONE=정답 선택지, OTHER=카탈로그에 없는 오개념 — 서로 다른 오개념이 OTHER 로 묶여
     * 거짓 반복 신호가 되는 것을 막기 위해 둘 다 제외한다.
     */
    private static final Set<String> NON_MISCONCEPTION_CODES = Set.of("NONE", "OTHER");

    private final QuestionDiagnosticProfileRepository questionProfileRepository;
    private final ConceptDiagnosticProfileRepository conceptProfileRepository;
    private final WeaknessAnalysisProperties props;
    private final ObjectMapper objectMapper;

    public boolean isEnabled() {
        String version = props.getDiagnosticProfileVersion();
        return version != null && !DISABLED.equalsIgnoreCase(version);
    }

    /** 문항별 프로필(스킬 유형 + 선택지→오개념 코드)을 questionId 로 조회한다. */
    public Map<Long, QuestionProfileView> loadQuestionProfiles(Collection<Long> questionIds) {
        if (!isEnabled() || questionIds == null || questionIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, QuestionProfileView> views = new LinkedHashMap<>();
        for (QuestionDiagnosticProfile profile : questionProfileRepository
                .findByQuestionIdInAndReviewStatusAndDiagnosticProfileVersion(
                        questionIds, DiagnosticReviewStatus.APPROVED, props.getDiagnosticProfileVersion())) {
            views.put(profile.getQuestionId(), toQuestionView(profile));
        }
        return views;
    }

    /** 개념별 프로필(핵심·설명·혼동 비교·기초 복습 등 학습 콘텐츠)을 조회한다. */
    public Optional<ConceptProfileView> loadConceptProfile(Long conceptId) {
        if (!isEnabled() || conceptId == null) {
            return Optional.empty();
        }
        return conceptProfileRepository
                .findByConceptIdAndReviewStatusAndDiagnosticProfileVersion(
                        conceptId, DiagnosticReviewStatus.APPROVED, props.getDiagnosticProfileVersion())
                .map(this::toConceptView);
    }

    private QuestionProfileView toQuestionView(QuestionDiagnosticProfile profile) {
        Map<String, String> misconceptionByOption = new LinkedHashMap<>();
        for (AnswerMisconception entry : readList(
                profile.getAnswerMisconceptionMap(), new TypeReference<List<AnswerMisconception>>() {})) {
            if (entry.optionKey() != null && entry.misconceptionCode() != null) {
                misconceptionByOption.put(entry.optionKey(), entry.misconceptionCode());
            }
        }
        return new QuestionProfileView(profile.getQuestionSkillType(), misconceptionByOption);
    }

    private ConceptProfileView toConceptView(ConceptDiagnosticProfile profile) {
        List<ConceptLearningResponse.ConfusionComparison> comparisons =
                readList(profile.getConfusionComparisons(), new TypeReference<List<ConfusionComparisonRaw>>() {})
                        .stream()
                        .map(raw -> new ConceptLearningResponse.ConfusionComparison(
                                raw.comparedWith(), raw.difference()))
                        .toList();
        return new ConceptProfileView(
                profile.getConceptCore(),
                profile.getConceptExplanation(),
                readList(profile.getKeyPoints(), new TypeReference<List<String>>() {}),
                comparisons,
                readList(profile.getPrerequisiteConcepts(), new TypeReference<List<String>>() {}),
                profile.getLearningExample(),
                profile.getMisconceptionNotes(),
                readList(profile.getBasicReviewPoints(), new TypeReference<List<String>>() {}));
    }

    /** TEXT 컬럼에 담긴 JSON 배열을 읽는다. 비어 있거나 형식이 어긋나면 빈 목록으로 처리한다. */
    private <T> List<T> readList(String json, TypeReference<List<T>> type) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    /** 문항 프로필 조회 결과. misconceptionByOption: 선택지 키 → 오개념 코드. */
    public record QuestionProfileView(String skillType, Map<String, String> misconceptionByOption) {

        /** 정답 표시값(NONE)·미분류(OTHER)는 오개념 코드로 보지 않는다. */
        public String misconceptionCodeFor(String selectedAnswer) {
            if (selectedAnswer == null) {
                return null;
            }
            String code = misconceptionByOption.get(selectedAnswer);
            return code == null || NON_MISCONCEPTION_CODES.contains(code) ? null : code;
        }
    }

    /** 개념 프로필 조회 결과(학습 콘텐츠). */
    public record ConceptProfileView(
            String conceptCore,
            String conceptExplanation,
            List<String> keyPoints,
            List<ConceptLearningResponse.ConfusionComparison> confusionComparisons,
            List<String> prerequisiteConcepts,
            String learningExample,
            String misconceptionNotes,
            List<String> basicReviewPoints) {

        public ConceptLearningResponse toResponse() {
            return new ConceptLearningResponse(conceptCore, conceptExplanation, keyPoints, confusionComparisons,
                    prerequisiteConcepts, learningExample, misconceptionNotes, basicReviewPoints);
        }
    }

    /** answer_misconception_map 원본 항목(와이어 포맷 snake_case). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AnswerMisconception(
            @JsonProperty("option_key") String optionKey,
            @JsonProperty("is_correct") Boolean isCorrect,
            @JsonProperty("interpretation") String interpretation,
            @JsonProperty("misconception_code") String misconceptionCode) {
    }

    /** confusion_comparisons 원본 항목(와이어 포맷 snake_case). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ConfusionComparisonRaw(
            @JsonProperty("compared_with") String comparedWith,
            @JsonProperty("difference") String difference) {
    }
}
