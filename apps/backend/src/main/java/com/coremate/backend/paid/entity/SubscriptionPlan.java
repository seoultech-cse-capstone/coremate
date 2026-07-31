package com.coremate.backend.paid.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 프리미엄 구독 상품.
 *
 * <p>일일 코인, 가입/월간 보너스, 분석표 할인·월간 한도, 랜덤박스 할인율을 담는다.
 * 실제 결제(PG) 연동은 후순위라 원화 금액({@code price_krw})은 아직 확정되지 않았고,
 * 스토어 수수료·부가세 정책이 정해지면 채운다.
 */
@Entity
@Table(name = "subscription_plans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_plan_id")
    private Long subscriptionPlanId;

    @Column(name = "plan_code", nullable = false, unique = true, length = 50)
    private String planCode;

    @Column(name = "plan_name", nullable = false, length = 100)
    private String planName;

    /** 월 구독 금액(원). 결제 정책 확정 전까지 null. */
    @Column(name = "price_krw")
    private Integer priceKrw;

    /** 구독자에게 매일 지급하는 코인. */
    @Column(name = "daily_coin", nullable = false)
    private int dailyCoin;

    /** 최초 가입 시 1회 지급 코인. */
    @Column(name = "signup_bonus_coin", nullable = false)
    private int signupBonusCoin;

    /** 구독 유지 시 월 1회 지급 코인. */
    @Column(name = "monthly_bonus_coin", nullable = false)
    private int monthlyBonusCoin;

    /** 랜덤박스 할인율(%). */
    @Column(name = "random_box_discount_rate", nullable = false)
    private int randomBoxDiscountRate;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
