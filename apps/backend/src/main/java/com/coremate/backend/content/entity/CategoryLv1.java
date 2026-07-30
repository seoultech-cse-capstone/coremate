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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "category_lv1",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_category_lv1_code",
                        columnNames = {"subject_id", "category_lv1_code"}),
                @UniqueConstraint(name = "uq_category_lv1_name",
                        columnNames = {"subject_id", "category_lv1_name"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryLv1 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_lv1_id")
    private Long categoryLv1Id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "category_lv1_code", nullable = false, length = 50)
    private String categoryLv1Code;

    @Column(name = "category_lv1_name", nullable = false, length = 100)
    private String categoryLv1Name;

    @Column(name = "category_lv1_name_en", length = 100)
    private String categoryLv1NameEn;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

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
