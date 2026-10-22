package com.coremate.backend.reward.repository;

import com.coremate.backend.reward.entity.AdRewardLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdRewardLogRepository extends JpaRepository<AdRewardLog, Long> {

    boolean existsByProviderTransactionId(String providerTransactionId);
}
