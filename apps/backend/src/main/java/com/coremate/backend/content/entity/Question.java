package com.coremate.backend.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "question")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concept_id", nullable = false)
    private Concept concept;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hint_common_mistake_id")
    private CommonMistake hintCommonMistake;

    @Column(name = "question_format", nullable = false, length = 30)
    private String questionFormat;

    @Column(name = "question_type", nullable = false, length = 30)
    private String questionType;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "choices_json", columnDefinition = "json")
    private String choicesJson;

    @Column(name = "correct_answer", nullable = false, length = 255)
    private String correctAnswer;

    @Column(name = "explanation_text", nullable = false, columnDefinition = "TEXT")
    private String explanationText;

    @Column(name = "difficulty_level", nullable = false)
    private short difficultyLevel;

    @Column(name = "difficulty_source", nullable = false, length = 20)
    private String difficultySource;

    @Column(name = "expected_solve_sec")
    private Integer expectedSolveSec;

    @Column(name = "content_hash", length = 64, unique = true)
    private String contentHash;

    @Column(name = "review_status", nullable = false, length = 20)
    private String reviewStatus;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType;

    @Column(name = "source_refs", columnDefinition = "json")
    private String sourceRefs;

    @Column(name = "is_daily_quiz_eligible", nullable = false)
    private boolean dailyQuizEligible;

    @Column(name = "is_mock_eligible", nullable = false)
    private boolean mockEligible;

    @Column(name = "is_concept_practice_eligible", nullable = false)
    private boolean conceptPracticeEligible;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
}
