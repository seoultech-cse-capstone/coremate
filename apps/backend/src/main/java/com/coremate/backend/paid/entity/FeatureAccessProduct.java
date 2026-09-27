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
 * AI 약점 분석표 생성 같은 유료 기능의 가격 정책.
 *
 * <p>가격을 코드 상수가 아니라 이 행에서 읽어, 값이 바뀌어도 배포 없이
 * 조정할 수 있게 한다. 시드는 {@code db/seed/02_feature_access_products.sql} 이다.
 *
 * <p>월간 생성 한도는 두지 않는다(2026-08-11 결정). 코인 가격과 하루 코인 수입 상한이
 * 이미 생성 빈도의 상한 역할을 한다.
 *
 * <p>MVP 기준 완료된 문제집 기록/해설 확인과 생성된 분석표 재열람은 유료 기능이 아니다.
 */
@Entity
@Table(name = "feature_access_products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class FeatureAccessProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feature_product_id")
    private Long featureProductId;

    /** 유료 기능 유형. MVP 기준 WEAKNESS_ANALYSIS_CREATE 중심. */
    @Column(name = "feature_type", nullable = false, unique = true, length = 50)
    private String featureType;

    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    /** 무과금 사용자 기준 코인 가격. */
    @Column(name = "price_coin", nullable = false)
    private int priceCoin;

    /** 프리미엄 사용자 할인 가격. 없으면 할인 없음. */
    @Column(name = "premium_price_coin")
    private Integer premiumPriceCoin;

    @Column(name = "access_policy", nullable = false, length = 30)
    private String accessPolicy;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 사용자 등급에 맞는 가격. 프리미엄 가격이 없으면 기본 가격을 쓴다. */
    public int priceFor(boolean premium) {
        if (premium && premiumPriceCoin != null) {
            return premiumPriceCoin;
        }
        return priceCoin;
    }
}
