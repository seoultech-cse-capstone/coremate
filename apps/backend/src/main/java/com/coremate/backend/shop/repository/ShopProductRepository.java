package com.coremate.backend.shop.repository;

import com.coremate.backend.shop.entity.ShopProduct;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopProductRepository extends JpaRepository<ShopProduct, Long> {

    @EntityGraph(attributePaths = {"item", "randomBox"})
    List<ShopProduct> findByActiveTrueOrderByRequiredLevelAscShopProductIdAsc();

    @EntityGraph(attributePaths = {"item", "randomBox"})
    Optional<ShopProduct> findByShopProductIdAndActiveTrue(Long shopProductId);

    @EntityGraph(attributePaths = {"item", "randomBox"})
    Optional<ShopProduct> findFirstByRandomBox_RandomBoxIdAndActiveTrueOrderByPriceCoinAsc(Long randomBoxId);
}
