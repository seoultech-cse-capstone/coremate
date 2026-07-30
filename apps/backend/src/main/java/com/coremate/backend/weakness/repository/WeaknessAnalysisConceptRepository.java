package com.coremate.backend.weakness.repository;

import com.coremate.backend.weakness.entity.WeaknessAnalysisConcept;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeaknessAnalysisConceptRepository extends JpaRepository<WeaknessAnalysisConcept, Long> {

    // 특정 분석표의 개념별 진단을 정렬 순서대로 조회한다.
    List<WeaknessAnalysisConcept> findByReport_AnalysisReportIdOrderBySortOrderAsc(Long reportId);
}
