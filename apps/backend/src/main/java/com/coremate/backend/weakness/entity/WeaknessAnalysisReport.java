package com.coremate.backend.weakness.entity;

import com.coremate.backend.weakness.dto.type.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 약점 분석표 본문. 요청/생성/조회 수명주기와 스냅샷/버전 메타데이터를 보관한다.
@Entity
@Table(name = "weakness_analysis_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class WeaknessAnalysisReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_report_id")
    private Long analysisReportId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "category_lv1_id")
    private Long categoryLv1Id;

    @Column(name = "category_lv2_id")
    private Long categoryLv2Id;

    @Column(name = "concept_id")
    private Long conceptId;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_scope_type", length = 30)
    private AnalysisScopeType analysisScopeType;

    @Column(name = "analysis_scope_name_snapshot", length = 200)
    private String analysisScopeNameSnapshot;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "summary_text", columnDefinition = "TEXT")
    private String summaryText;

    @Column(name = "analysis_text", columnDefinition = "TEXT")
    private String analysisText;

    @Column(name = "report_format", length = 20)
    private String reportFormat;

    @Column(name = "viewer_content_type", length = 30)
    private String viewerContentType;

    @Column(name = "selected_session_count", nullable = false)
    private int selectedSessionCount;

    @Column(name = "total_question_count", nullable = false)
    private int totalQuestionCount;

    @Column(name = "total_wrong_count", nullable = false)
    private int totalWrongCount;

    @Column(name = "total_long_time_count", nullable = false)
    private int totalLongTimeCount;

    @Column(name = "total_hint_used_count", nullable = false)
    private int totalHintUsedCount;

    @Column(name = "coin_cost", nullable = false)
    private int coinCost;

    @Column(name = "currency_log_id")
    private Long currencyLogId;

    @Column(name = "feature_product_id")
    private Long featureProductId;

    @Column(name = "ai_model", length = 50)
    private String aiModel;

    @Column(name = "prompt_version", length = 30)
    private String promptVersion;

    @Column(name = "schema_version", length = 30)
    private String schemaVersion;

    @Column(name = "scoring_rule_version", length = 30)
    private String scoringRuleVersion;

    @Column(name = "diagnostic_profile_version", length = 30)
    private String diagnosticProfileVersion;

    @Column(name = "request_fingerprint", length = 64)
    private String requestFingerprint;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "first_viewed_at")
    private LocalDateTime firstViewedAt;

    @Column(name = "last_viewed_at")
    private LocalDateTime lastViewedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AnalysisReportStatus status;

    @Column(name = "failed_reason", length = 255)
    private String failedReason;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private WeaknessAnalysisReport(Long userId, Long subjectId, Long categoryLv1Id, Long categoryLv2Id,
            Long conceptId, AnalysisScopeType analysisScopeType, String analysisScopeNameSnapshot,
            String title, String reportFormat, int selectedSessionCount, int totalQuestionCount,
            int totalWrongCount, int totalLongTimeCount, int totalHintUsedCount, int coinCost,
            String requestFingerprint) {
        this.userId = userId;
        this.subjectId = subjectId;
        this.categoryLv1Id = categoryLv1Id;
        this.categoryLv2Id = categoryLv2Id;
        this.conceptId = conceptId;
        this.analysisScopeType = analysisScopeType;
        this.analysisScopeNameSnapshot = analysisScopeNameSnapshot;
        this.title = title;
        this.reportFormat = reportFormat;
        this.selectedSessionCount = selectedSessionCount;
        this.totalQuestionCount = totalQuestionCount;
        this.totalWrongCount = totalWrongCount;
        this.totalLongTimeCount = totalLongTimeCount;
        this.totalHintUsedCount = totalHintUsedCount;
        this.coinCost = coinCost;
        this.requestFingerprint = requestFingerprint;
        this.status = AnalysisReportStatus.REQUESTED;
        this.requestedAt = LocalDateTime.now();
        this.viewCount = 0;
    }

    // AI 분석 생성을 시작한다.
    public void markGenerating() {
        this.status = AnalysisReportStatus.GENERATING;
    }

    // AI 분석 본문과 버전 메타데이터를 채우고 완료 처리한다.
    public void markCompleted(String summaryText, String analysisText, String aiModel, String promptVersion,
            String schemaVersion, String scoringRuleVersion, String diagnosticProfileVersion) {
        this.summaryText = summaryText;
        this.analysisText = analysisText;
        this.aiModel = aiModel;
        this.promptVersion = promptVersion;
        this.schemaVersion = schemaVersion;
        this.scoringRuleVersion = scoringRuleVersion;
        this.diagnosticProfileVersion = diagnosticProfileVersion;
        this.status = AnalysisReportStatus.COMPLETED;
        this.generatedAt = LocalDateTime.now();
    }

    // 실패 사유를 남기고 실패 처리한다.
    public void markFailed(String reason) {
        this.status = AnalysisReportStatus.FAILED;
        this.failedReason = reason;
    }

    // 조회수를 올리고 최초/최근 조회 시각을 기록한다.
    public void recordView() {
        this.viewCount++;
        LocalDateTime now = LocalDateTime.now();
        this.lastViewedAt = now;
        if (this.firstViewedAt == null) {
            this.firstViewedAt = now;
        }
    }

    // 소프트 삭제 처리한다.
    public void softDelete() {
        this.status = AnalysisReportStatus.DELETED;
    }
}
