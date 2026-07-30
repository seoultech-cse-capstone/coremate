package com.coremate.backend.weakness.repository;

import com.coremate.backend.weakness.entity.WeaknessAnalysisAiCallLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeaknessAnalysisAiCallLogRepository extends JpaRepository<WeaknessAnalysisAiCallLog, Long> {

    List<WeaknessAnalysisAiCallLog> findByAnalysisReportId(Long analysisReportId);
}
