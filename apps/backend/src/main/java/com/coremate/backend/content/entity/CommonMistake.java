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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Index;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "common_mistake",
        indexes = @Index(name = "idx_common_mistake_concept_title",
                columnList = "concept_id, mistake_title"),
        uniqueConstraints = @UniqueConstraint(name = "uq_common_mistake_code",
                columnNames = "mistake_code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommonMistake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "common_mistake_id")
    private Long commonMistakeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concept_id", nullable = false)
    private Concept concept;

    /**
     * AI 파이프라인이 부여한 오개념 식별자(예: {@code ALG-C-01-01-01_M02}).
     * {@code question_diagnostic_profile.answer_misconception_map} 의 오개념 코드와 같은 값이라,
     * 분석표가 반복된 오개념을 제목으로 보여 줄 때 이 컬럼으로 잇는다. 개발용 더미 행은 비어 있다.
     */
    @Column(name = "mistake_code", length = 50)
    private String mistakeCode;

    @Column(name = "mistake_title", nullable = false, length = 200)
    private String mistakeTitle;

    @Column(name = "mistake_text", nullable = false, columnDefinition = "TEXT")
    private String mistakeText;

    @Column(name = "correction_text", nullable = false, columnDefinition = "TEXT")
    private String correctionText;

    @Column(name = "review_status", nullable = false, length = 20)
    private String reviewStatus;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    // 콘텐츠 테이블은 AI 파이프라인(09/11 임포터)도 직접 INSERT 하므로 DB 기본값을 둔다.
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "datetime(6) not null default current_timestamp(6)")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "datetime(6) not null default current_timestamp(6) on update current_timestamp(6)")
    private LocalDateTime updatedAt;
}
