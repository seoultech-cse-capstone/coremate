package com.coremate.backend.paid.service;

import com.coremate.backend.paid.entity.FeatureAccessProduct;
import com.coremate.backend.paid.repository.FeatureAccessProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 유료 기능 가격을 정책 데이터에서 읽는다.
 *
 * <p>가격을 코드 상수가 아니라 {@code feature_access_products} 행에서 읽어,
 * 값이 바뀌어도 배포 없이 조정할 수 있게 한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureAccessPricingService {

    public static final String WEAKNESS_ANALYSIS_CREATE = "WEAKNESS_ANALYSIS_CREATE";

    private final FeatureAccessProductRepository featureAccessProductRepository;

    /**
     * 기능의 코인 가격. 정책 행이 없으면 {@code fallbackPrice} 를 쓴다.
     *
     * <p>시드가 적용되지 않은 환경에서도 기능이 멈추지 않도록 폴백을 둔다.
     * 폴백을 타면 정책 데이터가 빠졌다는 뜻이라 경고를 남긴다.
     */
    @Transactional(readOnly = true)
    public int priceCoinOf(String featureType, boolean premium, int fallbackPrice) {
        return featureAccessProductRepository.findByFeatureTypeAndActiveTrue(featureType)
                .map(product -> product.priceFor(premium))
                .orElseGet(() -> {
                    log.warn("유료 기능 가격 정책이 없어 기본값을 쓴다. featureType={}, fallback={}",
                            featureType, fallbackPrice);
                    return fallbackPrice;
                });
    }

    /** 월간 생성 한도. 정책 행이 없거나 한도가 없으면 null(무제한). */
    @Transactional(readOnly = true)
    public Integer monthlyLimitOf(String featureType, boolean premium) {
        return featureAccessProductRepository.findByFeatureTypeAndActiveTrue(featureType)
                .map(product -> product.monthlyLimitFor(premium))
                .orElse(null);
    }
}
