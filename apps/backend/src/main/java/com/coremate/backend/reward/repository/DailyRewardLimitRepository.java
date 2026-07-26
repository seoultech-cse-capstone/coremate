package com.coremate.backend.reward.repository;

import com.coremate.backend.reward.entity.DailyRewardLimit;
import com.coremate.backend.reward.entity.type.RewardType;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyRewardLimitRepository extends JpaRepository<DailyRewardLimit, Long> {

    List<DailyRewardLimit> findByUserIdOrderByRewardDateDescRewardTypeAsc(Long userId);

    List<DailyRewardLimit> findByUserIdAndRewardDateOrderByRewardTypeAsc(Long userId, LocalDate rewardDate);

    // 일일 한도 카운트를 올릴 때 같은 제한 행을 동시에 수정하지 못하게 잠근다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select drl from DailyRewardLimit drl
            where drl.userId = :userId
              and drl.rewardDate = :rewardDate
              and drl.rewardType = :rewardType
            """)
    Optional<DailyRewardLimit> findByUserIdAndRewardDateAndRewardTypeForUpdate(
            @Param("userId") Long userId,
            @Param("rewardDate") LocalDate rewardDate,
            @Param("rewardType") RewardType rewardType
    );
}
