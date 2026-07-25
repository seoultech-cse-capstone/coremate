package com.coremate.backend.weakness.entity;

import com.coremate.backend.weakness.dto.type.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 약점 분석표의 개념별 진단 결과. 정량 지표와 AI 해석/교차 검증 결과를 담는다.
@Entity
@Table(name = "weakness_analysis_concepts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class WeaknessAnalysisConcept {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_concept_id")
    private Long analysisConceptId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_report_id", nullable = false)
    private WeaknessAnalysisReport report;

    @Column(name = "concept_id", nullable = false)
    private Long conceptId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "weakness_score", nullable = false)
    private int weaknessScore;

    @Column(name = "evidence_strength", nullable = false, precision = 4, scale = 3)
    private BigDecimal evidenceStrength;

    @Column(name = "type_separation", precision = 4, scale = 3)
    private BigDecimal typeSeparation;

    @Enumerated(EnumType.STRING)
    @Column(name = "backend_candidate_type", length = 30)
    private WeaknessType backendCandidateType;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_primary_type", length = 30)
    private WeaknessType aiPrimaryType;

    @Column(name = "ai_secondary_signals", columnDefinition = "TEXT")
    private String aiSecondarySignals;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_analysis_status", length = 30)
    private AiAnalysisStatus aiAnalysisStatus;

    @Column(name = "type_agreement")
    private Boolean typeAgreement;

    @Enumerated(EnumType.STRING)
    @Column(name = "final_type", length = 30)
    private WeaknessType finalType;

    @Enumerated(EnumType.STRING)
    @Column(name = "final_type_source", length = 30)
    private FinalTypeSource finalTypeSource;

    @Column(name = "reconciliation_reason", length = 255)
    private String reconciliationReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "cause_code", length = 30)
    private CauseCode causeCode;

    @Column(name = "confused_relation", length = 255)
    private String confusedRelation;

    @Column(name = "diagnosis", columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "likely_cause", columnDefinition = "TEXT")
    private String likelyCause;

    @Column(name = "concept_tip", columnDefinition = "TEXT")
    private String conceptTip;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private WeaknessAnalysisConcept(WeaknessAnalysisReport report, Long conceptId, int sortOrder,
            int weaknessScore, BigDecimal evidenceStrength, BigDecimal typeSeparation,
            WeaknessType backendCandidateType, WeaknessType aiPrimaryType, String aiSecondarySignals,
            AiAnalysisStatus aiAnalysisStatus, Boolean typeAgreement, WeaknessType finalType,
            FinalTypeSource finalTypeSource, String reconciliationReason, CauseCode causeCode,
            String confusedRelation, String diagnosis, String likelyCause, String conceptTip) {
        this.report = report;
        this.conceptId = conceptId;
        this.sortOrder = sortOrder;
        this.weaknessScore = weaknessScore;
        this.evidenceStrength = evidenceStrength;
        this.typeSeparation = typeSeparation;
        this.backendCandidateType = backendCandidateType;
        this.aiPrimaryType = aiPrimaryType;
        this.aiSecondarySignals = aiSecondarySignals;
        this.aiAnalysisStatus = aiAnalysisStatus;
        this.typeAgreement = typeAgreement;
        this.finalType = finalType;
        this.finalTypeSource = finalTypeSource;
        this.reconciliationReason = reconciliationReason;
        this.causeCode = causeCode;
        this.confusedRelation = confusedRelation;
        this.diagnosis = diagnosis;
        this.likelyCause = likelyCause;
        this.conceptTip = conceptTip;
    }
}
