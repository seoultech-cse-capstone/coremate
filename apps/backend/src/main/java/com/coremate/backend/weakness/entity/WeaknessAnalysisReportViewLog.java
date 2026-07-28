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

// 약점 분석 리포트 열람 이벤트를 기록하는 조회 로그.
@Entity
@Table(name = "weakness_analysis_report_view_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class WeaknessAnalysisReportViewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_view_log_id")
    private Long reportViewLogId;

    @Column(name = "analysis_report_id", nullable = false)
    private Long analysisReportId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "view_type", length = 20)
    private String viewType;

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private WeaknessAnalysisReportViewLog(Long analysisReportId, Long userId) {
        this.analysisReportId = analysisReportId;
        this.userId = userId;
        this.viewType = "APP_VIEWER";
        this.viewedAt = LocalDateTime.now();
    }
}
