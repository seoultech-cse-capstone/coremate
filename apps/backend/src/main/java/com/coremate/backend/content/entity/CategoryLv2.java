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
@Table(name = "category_lv2")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryLv2 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_lv2_id")
    private Long categoryLv2Id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_lv1_id", nullable = false)
    private CategoryLv1 categoryLv1;

    @Column(name = "category_lv2_code", nullable = false, length = 50)
    private String categoryLv2Code;

    @Column(name = "category_lv2_name", nullable = false, length = 100)
    private String categoryLv2Name;

    @Column(name = "category_lv2_name_en", length = 100)
    private String categoryLv2NameEn;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
