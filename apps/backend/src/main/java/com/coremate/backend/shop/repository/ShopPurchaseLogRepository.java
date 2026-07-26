package com.coremate.backend.shop.repository;

import com.coremate.backend.shop.entity.ShopPurchaseLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopPurchaseLogRepository extends JpaRepository<ShopPurchaseLog, Long> {
}
