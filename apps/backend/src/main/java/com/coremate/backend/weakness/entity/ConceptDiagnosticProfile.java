package com.coremate.backend.weakness.entity;

import com.coremate.backend.weakness.dto.type.DiagnosticReviewStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 개념별 진단 프로필. 개념 핵심/설명/혼동 비교/선수 개념 등 AI 입력·학습 자료용 메타데이터를 보관한다.
@Entity
@Table(
        name = "concept_diagnostic_profile",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_concept_diagnostic_profile_concept_id",
                columnNames = "concept_id"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ConceptDiagnosticProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "concept_diagnostic_profile_id")
    private Long conceptDiagnosticProfileId;

    @Column(name = "concept_id", nullable = false)
    private Long conceptId;

    @Column(name = "concept_core", columnDefinition = "TEXT")
    private String conceptCore;

    @Column(name = "concept_explanation", columnDefinition = "TEXT")
    private String conceptExplanation;

    @Column(name = "key_points", columnDefinition = "TEXT")
    private String keyPoints;

    @Column(name = "confusion_comparisons", columnDefinition = "TEXT")
    private String confusionComparisons;

    @Column(name = "prerequisite_concepts", columnDefinition = "TEXT")
    private String prerequisiteConcepts;

    @Column(name = "learning_example", columnDefinition = "TEXT")
    private String learningExample;

    @Column(name = "misconception_notes", columnDefinition = "TEXT")
    private String misconceptionNotes;

    @Column(name = "basic_review_points", columnDefinition = "TEXT")
    private String basicReviewPoints;

    @Column(name = "misconception_mapping_coverage", precision = 4, scale = 3)
    private BigDecimal misconceptionMappingCoverage;

    @Column(name = "diagnostic_profile_version", length = 30)
    private String diagnosticProfileVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 30)
    private DiagnosticReviewStatus reviewStatus;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private ConceptDiagnosticProfile(Long conceptId, String conceptCore, String conceptExplanation,
            String keyPoints, String confusionComparisons, String prerequisiteConcepts,
            String learningExample, String misconceptionNotes, String basicReviewPoints,
            BigDecimal misconceptionMappingCoverage, String diagnosticProfileVersion) {
        this.conceptId = conceptId;
        this.conceptCore = conceptCore;
        this.conceptExplanation = conceptExplanation;
        this.keyPoints = keyPoints;
        this.confusionComparisons = confusionComparisons;
        this.prerequisiteConcepts = prerequisiteConcepts;
        this.learningExample = learningExample;
        this.misconceptionNotes = misconceptionNotes;
        this.basicReviewPoints = basicReviewPoints;
        this.misconceptionMappingCoverage = misconceptionMappingCoverage;
        this.diagnosticProfileVersion = diagnosticProfileVersion;
        this.reviewStatus = DiagnosticReviewStatus.DRAFT;
    }
}
