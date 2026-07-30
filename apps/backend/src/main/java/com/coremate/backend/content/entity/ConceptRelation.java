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
import java.time.LocalDateTime;
import org.hibernate.annotations.Check;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "concept_relation",
        uniqueConstraints = @UniqueConstraint(name = "uq_concept_relation",
                columnNames = {"source_concept_id", "target_concept_id", "relation_type"}))
@Check(name = "ck_concept_relation_not_self",
        constraints = "source_concept_id <> target_concept_id")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConceptRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "relation_id")
    private Long relationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_concept_id", nullable = false)
    private Concept sourceConcept;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_concept_id", nullable = false)
    private Concept targetConcept;

    @Column(name = "relation_type", nullable = false, length = 30)
    private String relationType;

    @Column(name = "relation_note", columnDefinition = "TEXT")
    private String relationNote;

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
