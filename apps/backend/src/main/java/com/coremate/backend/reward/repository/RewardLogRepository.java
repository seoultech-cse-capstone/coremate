package com.coremate.backend.reward.repository;

import com.coremate.backend.reward.entity.RewardLog;
import com.coremate.backend.reward.entity.type.RewardStatus;
import com.coremate.backend.reward.entity.type.RewardTargetType;
import com.coremate.backend.reward.entity.type.RewardType;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardLogRepository extends JpaRepository<RewardLog, Long> {

    List<RewardLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 챕터/과목 최초 코인 보상처럼 target 단위 중복 지급을 확인한다.
    boolean existsByUserIdAndRewardTypeAndTargetTypeAndTargetIdAndRewardStatus(
            Long userId,
            RewardType rewardType,
            RewardTargetType targetType,
            Long targetId,
            RewardStatus rewardStatus
    );
}
