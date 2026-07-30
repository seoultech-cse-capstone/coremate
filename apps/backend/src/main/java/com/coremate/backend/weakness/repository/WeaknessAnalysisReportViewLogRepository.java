package com.coremate.backend.weakness.repository;

import com.coremate.backend.weakness.entity.WeaknessAnalysisReportViewLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeaknessAnalysisReportViewLogRepository extends JpaRepository<WeaknessAnalysisReportViewLog, Long> {

    long countByAnalysisReportId(Long analysisReportId);
}
