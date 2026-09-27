package com.coremate.backend.progress.repository;

import com.coremate.backend.progress.entity.UserConceptProgress;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserConceptProgressRepository extends JpaRepository<UserConceptProgress, Long> {

    @Query("SELECT p.conceptId FROM UserConceptProgress p WHERE p.userId = :userId AND p.conceptId IN :conceptIds AND p.completed = true")
    Set<Long> findCompletedConceptIds(@Param("userId") Long userId, @Param("conceptIds") List<Long> conceptIds);

    Optional<UserConceptProgress> findByUserIdAndConceptId(Long userId, Long conceptId);
}
