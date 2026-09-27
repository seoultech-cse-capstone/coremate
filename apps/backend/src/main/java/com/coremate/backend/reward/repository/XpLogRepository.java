package com.coremate.backend.reward.repository;

import com.coremate.backend.reward.entity.XpLog;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface XpLogRepository extends JpaRepository<XpLog, Long> {

    List<XpLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
