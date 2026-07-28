package com.coremate.backend.weakness.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 약점 분석표에 포함된 풀이 세션과 선택 순서/스냅샷을 연결한다.
@Entity
@Table(
        name = "weakness_analysis_sessions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_weakness_analysis_sessions_report_session",
                columnNames = {"analysis_report_id", "solve_session_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class WeaknessAnalysisSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_report_session_id")
    private Long analysisReportSessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_report_id", nullable = false)
    private WeaknessAnalysisReport report;

    @Column(name = "solve_session_id", nullable = false)
    private Long solveSessionId;

    @Column(name = "selected_order", nullable = false)
    private int selectedOrder;

    @Column(name = "solve_session_title_snapshot", length = 200)
    private String solveSessionTitleSnapshot;

    @Column(name = "wrong_count_snapshot", nullable = false)
    private int wrongCountSnapshot;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private WeaknessAnalysisSession(WeaknessAnalysisReport report, Long solveSessionId, int selectedOrder,
            String solveSessionTitleSnapshot, int wrongCountSnapshot) {
        this.report = report;
        this.solveSessionId = solveSessionId;
        this.selectedOrder = selectedOrder;
        this.solveSessionTitleSnapshot = solveSessionTitleSnapshot;
        this.wrongCountSnapshot = wrongCountSnapshot;
    }
}
