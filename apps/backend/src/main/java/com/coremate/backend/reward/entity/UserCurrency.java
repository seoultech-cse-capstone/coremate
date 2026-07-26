package com.coremate.backend.reward.entity;

import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 사용자의 현재 코인 잔액 스냅샷. 모든 증감 이력은 CurrencyLog에 따로 남긴다.
@Entity
@Table(name = "user_currency")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserCurrency {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private int coin;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UserCurrency(Long userId) {
        this.userId = userId;
        this.coin = 0;
    }

    // 보상/환불/관리자 지급처럼 코인이 증가하는 경우.
    public void earn(int amount) {
        validatePositiveAmount(amount);
        this.coin += amount;
    }

    // 상점 구매/약점 분석표 생성처럼 코인이 차감되는 경우.
    public void spend(int amount) {
        validatePositiveAmount(amount);
        if (this.coin < amount) {
            throw new CoreMateException(ErrorCode.INSUFFICIENT_COIN);
        }
        this.coin -= amount;
    }

    private void validatePositiveAmount(int amount) {
        if (amount <= 0) {
            throw new CoreMateException(ErrorCode.INVALID_REWARD_AMOUNT);
        }
    }
}
