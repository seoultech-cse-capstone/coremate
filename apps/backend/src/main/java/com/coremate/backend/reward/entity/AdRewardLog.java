package com.coremate.backend.reward.entity;

import com.coremate.backend.reward.entity.type.AdRewardStatus;
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

// 광고 시청 보상 기록. 광고 제공자가 보낸 시청 완료 콜백 한 건이 한 행이다.
// provider_transaction_id 의 UNIQUE 는 db/migration/V7 이 만든다.
@Entity
@Table(name = "ad_reward_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AdRewardLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ad_reward_id")
    private Long adRewardId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "ad_provider", length = 50)
    private String adProvider;

    @Column(name = "ad_unit_id", length = 100)
    private String adUnitId;

    @Column(name = "provider_transaction_id", unique = true)
    private String providerTransactionId;

    @Column(name = "coin_amount", nullable = false)
    private int coinAmount;

    @Column(name = "currency_log_id")
    private Long currencyLogId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_status", nullable = false, length = 30)
    private AdRewardStatus rewardStatus;

    @Column(name = "rewarded_at", nullable = false)
    private LocalDateTime rewardedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 지급 판정 전에 먼저 저장한다. 행 ID 가 있어야 reward_logs·currency_logs 가 이 건을 가리킬 수 있다.
    // 미지급(FAILED)으로 시작하고, 지급되면 같은 트랜잭션에서 markRewarded 로 바꾼다.
    public static AdRewardLog received(Long userId, String adProvider, String adUnitId, String providerTransactionId) {
        AdRewardLog log = new AdRewardLog();
        log.userId = userId;
        log.adProvider = adProvider;
        log.adUnitId = adUnitId;
        log.providerTransactionId = providerTransactionId;
        log.coinAmount = 0;
        log.rewardStatus = AdRewardStatus.FAILED;
        log.rewardedAt = LocalDateTime.now();
        return log;
    }

    public void markRewarded(int coinAmount, Long currencyLogId) {
        this.coinAmount = coinAmount;
        this.currencyLogId = currencyLogId;
        this.rewardStatus = AdRewardStatus.REWARDED;
    }
}
