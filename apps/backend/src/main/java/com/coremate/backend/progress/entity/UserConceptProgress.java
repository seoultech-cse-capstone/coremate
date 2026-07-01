package com.coremate.backend.progress.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_concept_progress")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserConceptProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_concept_progress_id")
    private Long userConceptProgressId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "concept_id", nullable = false)
    private Long conceptId;

    @Column(name = "is_completed", nullable = false)
    private boolean completed;

    @Column(name = "completed_solve_session_id")
    private Long completedSolveSessionId;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
