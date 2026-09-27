package com.coremate.backend.solve.entity;

import com.coremate.backend.content.entity.Question;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "solve_logs",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"solve_session_id", "question_id"}),
                @UniqueConstraint(columnNames = {"solve_session_id", "question_order"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SolveLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "solve_log_id")
    private Long solveLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solve_session_id", nullable = false)
    private SolveSession solveSession;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "question_order", nullable = false)
    private int questionOrder;

    @Column(name = "selected_answer", length = 255)
    private String selectedAnswer;

    @Column(name = "correct_answer_snapshot", nullable = false, length = 255)
    private String correctAnswerSnapshot;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "time_spent_sec", nullable = false)
    private int timeSpentSec;

    @Column(name = "expected_solve_sec_snapshot")
    private Integer expectedSolveSecSnapshot;

    @Column(name = "hint_used", nullable = false)
    private boolean hintUsed;

    @Column(name = "hint_count", nullable = false)
    private int hintCount;

    @Column(name = "option_change_count", nullable = false)
    private int optionChangeCount;

    @Column(name = "is_answer_changed", nullable = false)
    private boolean answerChanged;

    @Column(name = "is_bookmarked", nullable = false)
    private boolean bookmarked;

    @Column(name = "solved_at", nullable = false)
    private LocalDateTime solvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private SolveLog(SolveSession solveSession, Long userId, Question question,
            int questionOrder, String selectedAnswer, String correctAnswerSnapshot,
            boolean correct, Integer timeSpentSec, Integer expectedSolveSecSnapshot,
            Boolean hintUsed, Integer hintCount, Integer optionChangeCount,
            Boolean answerChanged, Boolean bookmarked) {
        this.solveSession = solveSession;
        this.userId = userId;
        this.question = question;
        this.questionOrder = questionOrder;
        this.selectedAnswer = selectedAnswer;
        this.correctAnswerSnapshot = correctAnswerSnapshot;
        this.correct = correct;
        this.timeSpentSec = timeSpentSec == null ? 0 : timeSpentSec;
        this.expectedSolveSecSnapshot = expectedSolveSecSnapshot;
        this.hintUsed = hintUsed != null && hintUsed;
        this.hintCount = hintCount == null ? 0 : hintCount;
        this.optionChangeCount = optionChangeCount == null ? 0 : optionChangeCount;
        this.answerChanged = answerChanged != null && answerChanged;
        this.bookmarked = bookmarked != null && bookmarked;
        this.solvedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }
}
