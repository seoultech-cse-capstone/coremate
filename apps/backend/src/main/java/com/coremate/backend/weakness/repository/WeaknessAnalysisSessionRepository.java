package com.coremate.backend.weakness.repository;

import com.coremate.backend.weakness.entity.WeaknessAnalysisSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeaknessAnalysisSessionRepository extends JpaRepository<WeaknessAnalysisSession, Long> {

    // 특정 분석표에 포함된 세션을 선택 순서대로 조회한다.
    List<WeaknessAnalysisSession> findByReport_AnalysisReportIdOrderBySelectedOrderAsc(Long reportId);
}
