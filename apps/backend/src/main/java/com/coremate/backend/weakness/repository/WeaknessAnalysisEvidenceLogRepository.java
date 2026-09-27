package com.coremate.backend.weakness.repository;

import com.coremate.backend.weakness.entity.WeaknessAnalysisEvidenceLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeaknessAnalysisEvidenceLogRepository extends JpaRepository<WeaknessAnalysisEvidenceLog, Long> {

    List<WeaknessAnalysisEvidenceLog> findByAnalysisReportId(Long analysisReportId);
}
