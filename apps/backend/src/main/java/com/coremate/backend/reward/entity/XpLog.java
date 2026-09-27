package com.coremate.backend.reward.entity;

import com.coremate.backend.reward.entity.type.XpReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// XP 지급 내역. 레벨 계산과 성장 이력 추적의 기준이 된다.
@Entity
@Table(name = "xp_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class XpLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "xp_log_id")
    private Long xpLogId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "reward_policy_id")
    private Long rewardPolicyId;

    @Column(nullable = false)
    private int amount;

    @Column(name = "total_xp_after", nullable = false)
    private int totalXpAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private XpReason reason;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public XpLog(Long userId, Long rewardPolicyId, int amount, int totalXpAfter, XpReason reason,
                 String referenceType, Long referenceId) {
        this.userId = userId;
        this.rewardPolicyId = rewardPolicyId;
        this.amount = amount;
        this.totalXpAfter = totalXpAfter;
        this.reason = reason;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
    }
}
