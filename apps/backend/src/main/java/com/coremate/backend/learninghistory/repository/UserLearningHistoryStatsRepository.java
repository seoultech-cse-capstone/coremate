package com.coremate.backend.learninghistory.repository;

import com.coremate.backend.learninghistory.entity.UserLearningHistoryStats;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLearningHistoryStatsRepository extends JpaRepository<UserLearningHistoryStats, Long> {

    Optional<UserLearningHistoryStats> findByUserId(Long userId);
}
