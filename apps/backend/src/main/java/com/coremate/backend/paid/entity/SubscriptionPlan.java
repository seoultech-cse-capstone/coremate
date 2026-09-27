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
 * <p>일일 코인, 월간 보너스 코인, 랜덤박스 할인율을 담는다.
 * 실제 결제(PG) 연동은 후순위라 원화 금액({@code price_krw})은 아직 확정되지 않았고,
 * 스토어 수수료·부가세 정책이 정해지면 채운다.
 *
 * <p>여기에 담지 않는 것이 둘 있다.
 * <ul>
 *   <li><b>가입 보너스</b> — {@code reward_policies} 의 {@code PREMIUM_SIGNUP_BONUS} 행으로
 *       관리한다. 다른 일회성 보상과 같은 경로(지급 로그·중복 방지)를 타야 하기 때문이다.</li>
 *   <li><b>분석표 할인가</b> — {@code feature_access_products} 의 {@code premium_price_coin}
 *       으로 관리한다. 가격을 기능 쪽 한 곳에 모아 두어야 유료 기능이 늘어도 같은 구조로
 *       확장된다. 분석표 월간 생성 한도는 두지 않는다(2026-08-11 결정).</li>
 * </ul>
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
    @Column(name = "daily_coin_amount", nullable = false)
    private int dailyCoinAmount;

    /** 구독 유지 시 월 1회 지급 코인. */
    @Column(name = "monthly_bonus_coin", nullable = false)
    private int monthlyBonusCoin;

    /** 랜덤박스 할인율(%). */
    @Column(name = "random_box_discount_rate", nullable = false)
    private int randomBoxDiscountRate;

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
}
