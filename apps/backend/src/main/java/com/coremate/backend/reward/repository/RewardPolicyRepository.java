package com.coremate.backend.reward.repository;

import com.coremate.backend.reward.entity.RewardPolicy;
import com.coremate.backend.reward.entity.type.RewardType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardPolicyRepository extends JpaRepository<RewardPolicy, Long> {

    Optional<RewardPolicy> findByRewardTypeAndActiveTrue(RewardType rewardType);

    List<RewardPolicy> findByActiveTrueOrderByRewardTypeAsc();
}
