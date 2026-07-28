package com.coremate.backend.weakness.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// AI 약점 분석 호출의 요청 지문, 버전 메타데이터, 토큰 사용량을 기록하는 운영 로그.
@Entity
@Table(name = "weakness_analysis_ai_call_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class WeaknessAnalysisAiCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_call_log_id")
    private Long aiCallLogId;

    @Column(name = "analysis_report_id", nullable = false)
    private Long analysisReportId;

    @Column(name = "request_fingerprint", length = 64, nullable = false)
    private String requestFingerprint;

    @Column(name = "model", length = 50)
    private String model;

    @Column(name = "prompt_version", length = 30)
    private String promptVersion;

    @Column(name = "schema_version", length = 30)
    private String schemaVersion;

    @Column(name = "scoring_rule_version", length = 30)
    private String scoringRuleVersion;

    @Column(name = "diagnostic_profile_version", length = 30)
    private String diagnosticProfileVersion;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "cached_tokens")
    private Integer cachedTokens;

    @Column(name = "cache_write_tokens")
    private Integer cacheWriteTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "reasoning_tokens")
    private Integer reasoningTokens;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "escalation_count", nullable = false)
    private int escalationCount;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "status", length = 20)
    private String status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private WeaknessAnalysisAiCallLog(Long analysisReportId, String requestFingerprint, String model,
            String promptVersion, String schemaVersion, String scoringRuleVersion,
            String diagnosticProfileVersion, Integer inputTokens, Integer cachedTokens,
            Integer cacheWriteTokens, Integer outputTokens, Integer reasoningTokens,
            Integer retryCount, Integer escalationCount, Integer latencyMs, String status) {
        this.analysisReportId = analysisReportId;
        this.requestFingerprint = requestFingerprint;
        this.model = model;
        this.promptVersion = promptVersion;
        this.schemaVersion = schemaVersion;
        this.scoringRuleVersion = scoringRuleVersion;
        this.diagnosticProfileVersion = diagnosticProfileVersion;
        this.inputTokens = inputTokens;
        this.cachedTokens = cachedTokens;
        this.cacheWriteTokens = cacheWriteTokens;
        this.outputTokens = outputTokens;
        this.reasoningTokens = reasoningTokens;
        this.retryCount = retryCount == null ? 0 : retryCount;
        this.escalationCount = escalationCount == null ? 0 : escalationCount;
        this.latencyMs = latencyMs;
        this.status = status;
    }
}
