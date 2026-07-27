package com.coremate.backend.weakness.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coremate.backend.weakness.config.WeaknessAnalysisProperties;
import com.coremate.backend.weakness.dto.type.DiagnosticReviewStatus;
import com.coremate.backend.weakness.entity.ConceptDiagnosticProfile;
import com.coremate.backend.weakness.entity.QuestionDiagnosticProfile;
import com.coremate.backend.weakness.repository.ConceptDiagnosticProfileRepository;
import com.coremate.backend.weakness.repository.QuestionDiagnosticProfileRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import tools.jackson.databind.ObjectMapper;

/**
 * 진단 프로필 조회·파싱 규칙 검증. 특히 오개념 코드 필터(NONE/OTHER)와
 * 버전 비활성(none) 시 조회를 하지 않는지 확인한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DiagnosticProfileServiceTest {

    @Mock
    private QuestionDiagnosticProfileRepository questionProfileRepository;

    @Mock
    private ConceptDiagnosticProfileRepository conceptProfileRepository;

    private WeaknessAnalysisProperties props;
    private DiagnosticProfileService service;

    @BeforeEach
    void setUp() {
        props = new WeaknessAnalysisProperties();
        service = new DiagnosticProfileService(
                questionProfileRepository, conceptProfileRepository, props, new ObjectMapper());
    }

    private QuestionDiagnosticProfile questionProfile(String answerMap) {
        return QuestionDiagnosticProfile.builder()
                .questionId(9501L)
                .conceptId(9301L)
                .questionSkillType("RECALL")
                .answerMisconceptionMap(answerMap)
                .diagnosticProfileVersion("v1")
                .build();
    }

    @Test
    void mapsSkillTypeAndMisconceptionCodePerOption() {
        String answerMap = """
                [{"option_key":"A","is_correct":true,"interpretation":"정답","misconception_code":"NONE"},
                 {"option_key":"B","is_correct":false,"interpretation":"오답","misconception_code":"CPT_M01"}]
                """;
        when(questionProfileRepository.findByQuestionIdInAndReviewStatusAndDiagnosticProfileVersion(
                any(), any(), anyString())).thenReturn(List.of(questionProfile(answerMap)));

        Map<Long, DiagnosticProfileService.QuestionProfileView> views =
                service.loadQuestionProfiles(List.of(9501L));

        DiagnosticProfileService.QuestionProfileView view = views.get(9501L);
        assertThat(view.skillType()).isEqualTo("RECALL");
        assertThat(view.misconceptionCodeFor("B")).isEqualTo("CPT_M01");
    }

    @Test
    void treatsNoneAndOtherAsNoMisconceptionCode() {
        String answerMap = """
                [{"option_key":"A","is_correct":true,"interpretation":"정답","misconception_code":"NONE"},
                 {"option_key":"C","is_correct":false,"interpretation":"분류 밖","misconception_code":"OTHER"}]
                """;
        when(questionProfileRepository.findByQuestionIdInAndReviewStatusAndDiagnosticProfileVersion(
                any(), any(), anyString())).thenReturn(List.of(questionProfile(answerMap)));

        DiagnosticProfileService.QuestionProfileView view =
                service.loadQuestionProfiles(List.of(9501L)).get(9501L);

        // 정답 표시값(NONE)과 미분류(OTHER)는 반복 오개념 집계를 오염시키므로 코드로 보지 않는다.
        assertThat(view.misconceptionCodeFor("A")).isNull();
        assertThat(view.misconceptionCodeFor("C")).isNull();
        assertThat(view.misconceptionCodeFor("Z")).isNull();
    }

    @Test
    void toleratesBrokenJsonInsteadOfFailing() {
        when(questionProfileRepository.findByQuestionIdInAndReviewStatusAndDiagnosticProfileVersion(
                any(), any(), anyString())).thenReturn(List.of(questionProfile("{망가진 JSON")));

        DiagnosticProfileService.QuestionProfileView view =
                service.loadQuestionProfiles(List.of(9501L)).get(9501L);

        assertThat(view.skillType()).isEqualTo("RECALL");
        assertThat(view.misconceptionCodeFor("B")).isNull();
    }

    @Test
    void readsConceptLearningContent() {
        ConceptDiagnosticProfile profile = ConceptDiagnosticProfile.builder()
                .conceptId(9301L)
                .conceptCore("핵심")
                .conceptExplanation("설명")
                .keyPoints("[\"포인트1\",\"포인트2\"]")
                .confusionComparisons("[{\"compared_with\":\"포화 이진 트리\",\"difference\":\"차이\"}]")
                .prerequisiteConcepts("[\"이진 트리\"]")
                .learningExample("예시")
                .misconceptionNotes("오개념")
                .basicReviewPoints("[\"기초1\"]")
                .misconceptionMappingCoverage(new BigDecimal("1.000"))
                .diagnosticProfileVersion("v1")
                .build();
        when(conceptProfileRepository.findByConceptIdAndReviewStatusAndDiagnosticProfileVersion(
                anyLong(), any(), anyString())).thenReturn(Optional.of(profile));

        DiagnosticProfileService.ConceptProfileView view = service.loadConceptProfile(9301L).orElseThrow();

        assertThat(view.keyPoints()).containsExactly("포인트1", "포인트2");
        assertThat(view.confusionComparisons()).hasSize(1);
        assertThat(view.confusionComparisons().get(0).comparedWith()).isEqualTo("포화 이진 트리");
        assertThat(view.prerequisiteConcepts()).containsExactly("이진 트리");
        assertThat(view.toResponse().conceptExplanation()).isEqualTo("설명");
    }

    @Test
    void skipsLookupEntirelyWhenVersionIsNone() {
        props.setDiagnosticProfileVersion("none");

        assertThat(service.isEnabled()).isFalse();
        assertThat(service.loadQuestionProfiles(List.of(9501L))).isEmpty();
        assertThat(service.loadConceptProfile(9301L)).isEmpty();

        verify(questionProfileRepository, never())
                .findByQuestionIdInAndReviewStatusAndDiagnosticProfileVersion(any(), any(), anyString());
        verify(conceptProfileRepository, never())
                .findByConceptIdAndReviewStatusAndDiagnosticProfileVersion(anyLong(), any(), anyString());
    }

    @Test
    void requestsOnlyApprovedProfiles() {
        when(questionProfileRepository.findByQuestionIdInAndReviewStatusAndDiagnosticProfileVersion(
                any(), any(), anyString())).thenReturn(List.of());

        service.loadQuestionProfiles(List.of(9501L));

        verify(questionProfileRepository).findByQuestionIdInAndReviewStatusAndDiagnosticProfileVersion(
                List.of(9501L), DiagnosticReviewStatus.APPROVED, "v1");
    }
}
