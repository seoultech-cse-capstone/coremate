package com.coremate.backend.reward.entity;

import com.coremate.backend.reward.entity.type.RewardType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 사용자별 하루 보상/풀이 횟수 제한을 관리한다.
@Entity
@Table(
        name = "daily_reward_limits",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_daily_reward_limits",
                columnNames = {"user_id", "reward_date", "reward_type"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class DailyRewardLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_reward_limit_id")
    private Long dailyRewardLimitId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "reward_date", nullable = false)
    private LocalDate rewardDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false, length = 50)
    private RewardType rewardType;

    @Column(name = "reward_count", nullable = false)
    private int rewardCount;

    @Column(name = "daily_limit")
    private Integer dailyLimit;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public DailyRewardLimit(Long userId, LocalDate rewardDate, RewardType rewardType, Integer dailyLimit) {
        this.userId = userId;
        this.rewardDate = rewardDate;
        this.rewardType = rewardType;
        this.rewardCount = 0;
        this.dailyLimit = dailyLimit;
    }

    // dailyLimit이 없으면 무제한, 있으면 현재 카운트와 비교한다.
    public boolean canConsume() {
        return dailyLimit == null || rewardCount < dailyLimit;
    }

    // 보상 지급 또는 제한 대상 행동 1회를 기록한다.
    public void consume() {
        this.rewardCount += 1;
    }
}
