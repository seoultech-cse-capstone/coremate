package com.coremate.backend.reward.entity;

import com.coremate.backend.reward.entity.type.RewardSkipReason;
import com.coremate.backend.reward.entity.type.RewardSource;
import com.coremate.backend.reward.entity.type.RewardStatus;
import com.coremate.backend.reward.entity.type.RewardTargetType;
import com.coremate.backend.reward.entity.type.RewardType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// XP/코인/아이템/랜덤박스 보상 판정 결과를 통합으로 남기는 로그.
// 인덱스는 db/migration/V2 가 만든다. 여기 선언은 엔티티에서도 스키마가 보이게 하려는 것이다.
@Entity
@Table(
        name = "reward_logs",
        indexes = {
                @Index(name = "idx_reward_logs_scope", columnList = "user_id,reward_type,target_type,target_id,created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RewardLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reward_log_id")
    private Long rewardLogId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "reward_policy_id")
    private Long rewardPolicyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false, length = 50)
    private RewardType rewardType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_source", nullable = false, length = 50)
    private RewardSource rewardSource;

    @Column(name = "source_id")
    private Long sourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 50)
    private RewardTargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "solve_session_id")
    private Long solveSessionId;

    @Column(name = "xp_amount", nullable = false)
    private int xpAmount;

    @Column(name = "coin_amount", nullable = false)
    private int coinAmount;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "random_box_id")
    private Long randomBoxId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_status", nullable = false, length = 20)
    private RewardStatus rewardStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "skip_reason", length = 100)
    private RewardSkipReason skipReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 실제 지급이 끝난 보상 로그.
    public static RewardLog granted(Long userId, Long rewardPolicyId, RewardType rewardType,
                                    RewardSource rewardSource, Long sourceId, RewardTargetType targetType,
                                    Long targetId, Long solveSessionId, int xpAmount, int coinAmount,
                                    Long itemId, Long randomBoxId) {
        return new RewardLog(userId, rewardPolicyId, rewardType, rewardSource, sourceId, targetType, targetId,
                solveSessionId, xpAmount, coinAmount, itemId, randomBoxId, RewardStatus.GRANTED, null);
    }

    // 정책 없음처럼 policyId가 없는 미지급 로그.
    public static RewardLog skipped(Long userId, RewardType rewardType, RewardSource rewardSource, Long sourceId,
                                    RewardTargetType targetType, Long targetId, Long solveSessionId,
                                    RewardSkipReason skipReason) {
        return skipped(userId, null, rewardType, rewardSource, sourceId, targetType, targetId,
                solveSessionId, skipReason);
    }

    // 정책은 찾았지만 조건 미충족으로 지급하지 않은 로그.
    public static RewardLog skipped(Long userId, Long rewardPolicyId, RewardType rewardType,
                                    RewardSource rewardSource, Long sourceId, RewardTargetType targetType,
                                    Long targetId, Long solveSessionId, RewardSkipReason skipReason) {
        return new RewardLog(userId, rewardPolicyId, rewardType, rewardSource, sourceId, targetType, targetId,
                solveSessionId, 0, 0, null, null, RewardStatus.SKIPPED, skipReason);
    }

    private RewardLog(Long userId, Long rewardPolicyId, RewardType rewardType, RewardSource rewardSource,
                      Long sourceId, RewardTargetType targetType, Long targetId, Long solveSessionId,
                      int xpAmount, int coinAmount, Long itemId, Long randomBoxId, RewardStatus rewardStatus,
                      RewardSkipReason skipReason) {
        this.userId = userId;
        this.rewardPolicyId = rewardPolicyId;
        this.rewardType = rewardType;
        this.rewardSource = rewardSource;
        this.sourceId = sourceId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.solveSessionId = solveSessionId;
        this.xpAmount = xpAmount;
        this.coinAmount = coinAmount;
        this.itemId = itemId;
        this.randomBoxId = randomBoxId;
        this.rewardStatus = rewardStatus;
        this.skipReason = skipReason;
    }
}
