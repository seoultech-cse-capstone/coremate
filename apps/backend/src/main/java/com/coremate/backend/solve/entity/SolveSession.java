package com.coremate.backend.solve.entity;

import com.coremate.backend.content.entity.CategoryLv1;
import com.coremate.backend.content.entity.CategoryLv2;
import com.coremate.backend.content.entity.Concept;
import com.coremate.backend.content.entity.Subject;
import com.coremate.backend.solve.dto.type.SolveSessionStatus;
import com.coremate.backend.solve.dto.type.SolveSessionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "solve_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SolveSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "solve_session_id")
    private Long solveSessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 30)
    private SolveSessionType sessionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_lv1_id")
    private CategoryLv1 categoryLv1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_lv2_id")
    private CategoryLv2 categoryLv2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concept_id")
    private Concept concept;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "total_question_count", nullable = false)
    private int totalQuestionCount;

    /**
     * 세션 시작 시 결정된 문제 ID 목록 (JSON 배열).
     * 서버가 진실의 원천이 되어 답안 제출 시 questionOrder 결정과 범위 검증에 사용된다.
     */
    @Column(name = "question_ids_json", columnDefinition = "json")
    private String questionIdsJson;

    @Column(name = "correct_count", nullable = false)
    private int correctCount;

    @Column(name = "wrong_count", nullable = false)
    private int wrongCount;

    @Column(name = "correct_rate", precision = 5, scale = 2)
    private BigDecimal correctRate;

    @Column(name = "total_time_spent_sec", nullable = false)
    private int totalTimeSpentSec;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SolveSessionStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private SolveSession(Long userId, SolveSessionType sessionType, Subject subject,
            CategoryLv1 categoryLv1, CategoryLv2 categoryLv2, Concept concept,
            String title, int totalQuestionCount, String questionIdsJson) {
        this.userId = userId;
        this.sessionType = sessionType;
        this.subject = subject;
        this.categoryLv1 = categoryLv1;
        this.categoryLv2 = categoryLv2;
        this.concept = concept;
        this.title = title;
        this.totalQuestionCount = totalQuestionCount;
        this.questionIdsJson = questionIdsJson;
        this.correctCount = 0;
        this.wrongCount = 0;
        this.totalTimeSpentSec = 0;
        this.status = SolveSessionStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 회차를 중단 처리한다. 회차와 풀이 로그는 삭제하지 않고 상태만 바꾼다.
     * 중단된 회차는 완료 기록/해설, 약점 분석, 보상, 학습 통계 대상에서 제외된다.
     */
    public void abandon() {
        this.status = SolveSessionStatus.ABANDONED;
        this.updatedAt = LocalDateTime.now();
    }

    public void complete(int correctCount, int wrongCount, int totalTimeSpentSec) {
        this.correctCount = correctCount;
        this.wrongCount = wrongCount;
        this.correctRate = totalQuestionCount > 0
                ? BigDecimal.valueOf(correctCount * 100.0 / totalQuestionCount)
                        .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        this.totalTimeSpentSec = totalTimeSpentSec;
        this.status = SolveSessionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
