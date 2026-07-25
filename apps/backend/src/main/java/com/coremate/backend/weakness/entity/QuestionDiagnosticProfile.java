package com.coremate.backend.weakness.entity;

import com.coremate.backend.weakness.dto.type.DiagnosticReviewStatus;
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
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 문항별 진단 프로필. 문항의 스킬 유형·난이도·오답 오개념 맵 등 AI 입력용 메타데이터를 보관한다.
@Entity
@Table(
        name = "question_diagnostic_profile",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_question_diagnostic_profile_question_id",
                columnNames = "question_id"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class QuestionDiagnosticProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_diagnostic_profile_id")
    private Long questionDiagnosticProfileId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "concept_id")
    private Long conceptId;

    @Column(name = "diagnostic_summary", columnDefinition = "TEXT")
    private String diagnosticSummary;

    @Column(name = "question_skill_type", length = 40)
    private String questionSkillType;

    @Column(name = "expected_solve_sec")
    private Integer expectedSolveSec;

    @Column(name = "difficulty")
    private Integer difficulty;

    @Column(name = "answer_misconception_map", columnDefinition = "TEXT")
    private String answerMisconceptionMap;

    @Column(name = "diagnostic_profile_version", length = 30)
    private String diagnosticProfileVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 30)
    private DiagnosticReviewStatus reviewStatus;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private QuestionDiagnosticProfile(Long questionId, Long conceptId, String diagnosticSummary,
            String questionSkillType, Integer expectedSolveSec, Integer difficulty,
            String answerMisconceptionMap, String diagnosticProfileVersion) {
        this.questionId = questionId;
        this.conceptId = conceptId;
        this.diagnosticSummary = diagnosticSummary;
        this.questionSkillType = questionSkillType;
        this.expectedSolveSec = expectedSolveSec;
        this.difficulty = difficulty;
        this.answerMisconceptionMap = answerMisconceptionMap;
        this.diagnosticProfileVersion = diagnosticProfileVersion;
        this.reviewStatus = DiagnosticReviewStatus.DRAFT;
    }
}
