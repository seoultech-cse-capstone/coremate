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
@Table(name = "concept")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Concept {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "concept_id")
    private Long conceptId;

    @Column(name = "concept_code", nullable = false, unique = true, length = 100)
    private String conceptCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_lv2_id", nullable = false)
    private CategoryLv2 categoryLv2;

    @Column(name = "concept_name", nullable = false, length = 150)
    private String conceptName;

    @Column(name = "concept_name_en", length = 150)
    private String conceptNameEn;

    @Column(name = "aliases_json", columnDefinition = "json")
    private String aliasesJson;

    @Column(name = "one_line_definition", nullable = false, columnDefinition = "TEXT")
    private String oneLineDefinition;

    @Column(name = "core_summary", nullable = false, columnDefinition = "TEXT")
    private String coreSummary;

    @Column(name = "core_keywords_json", nullable = false, columnDefinition = "json")
    private String coreKeywordsJson;

    @Column(name = "key_properties_json", nullable = false, columnDefinition = "json")
    private String keyPropertiesJson;

    @Column(name = "source_refs", columnDefinition = "json")
    private String sourceRefs;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "review_status", nullable = false, length = 20)
    private String reviewStatus;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

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
