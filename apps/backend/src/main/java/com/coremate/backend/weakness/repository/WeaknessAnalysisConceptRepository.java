package com.coremate.backend.weakness.repository;

import com.coremate.backend.weakness.entity.WeaknessAnalysisConcept;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WeaknessAnalysisConceptRepository extends JpaRepository<WeaknessAnalysisConcept, Long> {

    // 특정 분석표의 개념별 진단을 정렬 순서대로 조회한다.
    List<WeaknessAnalysisConcept> findByReport_AnalysisReportIdOrderBySortOrderAsc(Long reportId);

    long countByReport_AnalysisReportId(Long reportId);

    /**
     * 분석표별 약점 개념 수를 한 번에 센다. 목록 화면이 분석표마다 "약점 몇 개"를 보여주는데,
     * 분석표 수만큼 count 를 날리면 N+1 이 되므로 묶어서 조회한다.
     */
    @Query("""
            select c.report.analysisReportId as reportId, count(c) as conceptCount
            from WeaknessAnalysisConcept c
            where c.report.analysisReportId in :reportIds
            group by c.report.analysisReportId
            """)
    List<ConceptCountByReport> countConceptsByReportIds(@Param("reportIds") List<Long> reportIds);

    interface ConceptCountByReport {
        Long getReportId();

        long getConceptCount();
    }
}
