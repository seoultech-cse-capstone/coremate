package com.coremate.backend.paid.repository;

import com.coremate.backend.paid.entity.FeatureAccessProduct;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureAccessProductRepository extends JpaRepository<FeatureAccessProduct, Long> {

    Optional<FeatureAccessProduct> findByFeatureTypeAndActiveTrue(String featureType);
}
