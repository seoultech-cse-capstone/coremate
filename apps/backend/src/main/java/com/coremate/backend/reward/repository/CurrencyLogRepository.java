package com.coremate.backend.reward.repository;

import com.coremate.backend.reward.entity.CurrencyLog;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyLogRepository extends JpaRepository<CurrencyLog, Long> {

    List<CurrencyLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
