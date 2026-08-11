package com.coremate.backend.reward.entity;

import com.coremate.backend.reward.entity.type.RewardKind;
import com.coremate.backend.reward.entity.type.RewardScope;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 어떤 행동에 XP/코인/아이템을 얼마나 지급할지 정하는 운영 정책.
@Entity
@Table(name = "reward_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RewardPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reward_policy_id")
    private Long rewardPolicyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false, unique = true, length = 50)
    private RewardType rewardType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_kind", nullable = false, length = 30)
    private RewardKind rewardKind;

    @Column(name = "coin_amount", nullable = false)
    private int coinAmount;

    @Column(name = "xp_amount", nullable = false)
    private int xpAmount;

    @Column(name = "daily_limit")
    private Integer dailyLimit;

    @Column(name = "min_correct_rate", precision = 5, scale = 2)
    private BigDecimal minCorrectRate;

    @Column(name = "first_reward_only", nullable = false)
    private boolean firstRewardOnly;

    // 같은 범위로 하루 1회만 지급할지. firstRewardOnly(평생 1회)와 다른 축이다.
    // 평생 1회를 켜면 반복 수입이 사라지고, 이쪽만 켜면 날짜가 바뀔 때 다시 받을 수 있다.
    @Column(name = "scope_daily_once", nullable = false)
    private boolean scopeDailyOnce;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_scope", length = 30)
    private RewardScope rewardScope;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean hasDailyLimit() {
        return dailyLimit != null;
    }
}
