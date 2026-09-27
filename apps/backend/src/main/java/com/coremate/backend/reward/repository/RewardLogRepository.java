package com.coremate.backend.reward.repository;

import com.coremate.backend.reward.entity.RewardLog;
import com.coremate.backend.reward.entity.type.RewardStatus;
import com.coremate.backend.reward.entity.type.RewardTargetType;
import com.coremate.backend.reward.entity.type.RewardType;
import java.time.LocalDateTime;
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

    // 위와 같은 판정에 "오늘" 조건만 더한 것. scope_daily_once 정책의 당일 중복 확인에 쓴다.
    // 별도 카운트 테이블을 두지 않고 지급 로그를 그대로 근거로 삼는다.
    boolean existsByUserIdAndRewardTypeAndTargetTypeAndTargetIdAndRewardStatusAndCreatedAtGreaterThanEqual(
            Long userId,
            RewardType rewardType,
            RewardTargetType targetType,
            Long targetId,
            RewardStatus rewardStatus,
            LocalDateTime createdAtFrom
    );
}
