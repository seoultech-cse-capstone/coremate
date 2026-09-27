package com.coremate.backend.dailyquiz.entity;

import com.coremate.backend.content.entity.Question;
import com.coremate.backend.content.entity.Subject;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 하루 단위로 사용자에게 제공되는 오늘의 CS 퀴즈 문제 구성.
@Entity
@Table(
        name = "daily_quiz_sets",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_daily_quiz_sets_date_question", columnNames = {"quiz_date", "question_id"}),
                @UniqueConstraint(name = "uq_daily_quiz_sets_date_order", columnNames = {"quiz_date", "sort_order"})
        },
        indexes = {
                @Index(name = "idx_daily_quiz_sets_question_id", columnList = "question_id"),
                @Index(name = "idx_daily_quiz_sets_subject_id", columnList = "subject_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class DailyQuizSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_quiz_set_id")
    private Long dailyQuizSetId;

    @Column(name = "quiz_date", nullable = false)
    private LocalDate quizDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public DailyQuizSet(LocalDate quizDate, Question question, Subject subject, int sortOrder) {
        this.quizDate = quizDate;
        this.question = question;
        this.subject = subject;
        this.sortOrder = sortOrder;
    }
}
