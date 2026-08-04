package com.coremate.backend.weakness.entity;

import com.coremate.backend.weakness.dto.type.EvidenceType;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 약점 분석 리포트가 근거로 삼은 개별 풀이 로그를 기록하는 감사 로그.
@Entity
@Table(
        name = "weakness_analysis_evidence_logs",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_weakness_evidence_report_solve_type",
                columnNames = {"analysis_report_id", "solve_log_id", "evidence_type"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class WeaknessAnalysisEvidenceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evidence_log_id")
    private Long evidenceLogId;

    @Column(name = "analysis_report_id", nullable = false)
    private Long analysisReportId;

    @Column(name = "analysis_concept_id")
    private Long analysisConceptId;

    @Column(name = "solve_log_id", nullable = false)
    private Long solveLogId;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", length = 20, nullable = false)
    private EvidenceType evidenceType;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private WeaknessAnalysisEvidenceLog(Long analysisReportId, Long analysisConceptId,
            Long solveLogId, EvidenceType evidenceType) {
        this.analysisReportId = analysisReportId;
        this.analysisConceptId = analysisConceptId;
        this.solveLogId = solveLogId;
        this.evidenceType = evidenceType;
    }
}
