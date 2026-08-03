package com.coremate.backend.dailyquiz.entity;

import com.coremate.backend.content.entity.Question;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 사용자가 제출한 오늘의 CS 퀴즈 문항별로 선택한 선지 기록. 결과/해설 조회 시 내가 고른 선지를 보여주는 데 사용된다.
@Entity
@Table(
        name = "daily_quiz_answer_logs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_daily_quiz_answer_logs_user_date_question",
                        columnNames = {"user_id", "quiz_date", "question_id"})
        },
        indexes = {
                @Index(name = "idx_daily_quiz_answer_logs_user_date", columnList = "user_id, quiz_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class DailyQuizAnswerLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_quiz_answer_log_id")
    private Long dailyQuizAnswerLogId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "quiz_date", nullable = false)
    private LocalDate quizDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "selected_answer", length = 255)
    private String selectedAnswer;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private DailyQuizAnswerLog(Long userId, LocalDate quizDate, Question question, int sortOrder,
            String selectedAnswer) {
        this.userId = userId;
        this.quizDate = quizDate;
        this.question = question;
        this.sortOrder = sortOrder;
        this.selectedAnswer = selectedAnswer;
    }
}
