package com.coremate.backend.weakness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.weakness.dto.type.AnalysisReportStatus;
import com.coremate.backend.weakness.dto.type.AnalysisScopeType;
import com.coremate.backend.weakness.entity.WeaknessAnalysisConcept;
import com.coremate.backend.weakness.entity.WeaknessAnalysisReport;
import com.coremate.backend.weakness.repository.WeaknessAnalysisConceptRepository;
import com.coremate.backend.weakness.repository.WeaknessAnalysisReportRepository;
import com.coremate.backend.weakness.service.WeaknessAnalysisService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 저장/조회 통합 테스트(H2). 약점분석 리포트를 직접 저장한 뒤 목록/상세(재열람 기록)/논리삭제
 * 왕복이 실제 JPA로 도는지 검증한다. (생성 흐름의 AI/코인은 별도 단위·통합 테스트에서 검증)
 */
@SpringBootTest
@Transactional
class WeaknessReportPersistenceIntegrationTest {

    @Autowired
    private WeaknessAnalysisService service;

    @Autowired
    private WeaknessAnalysisReportRepository reportRepository;

    @Autowired
    private WeaknessAnalysisConceptRepository conceptRepository;

    private Long reportId;

    @BeforeEach
    void seed() {
        WeaknessAnalysisReport report = WeaknessAnalysisReport.builder()
                .userId(1L)
                .subjectId(10L)
                .analysisScopeType(AnalysisScopeType.SUBJECT)
                .analysisScopeNameSnapshot("자료구조")
                .title("테스트 약점 분석표")
                .reportFormat("MARKDOWN")
                .selectedSessionCount(2)
                .totalQuestionCount(10)
                .totalWrongCount(4)
                .totalLongTimeCount(2)
                .totalHintUsedCount(1)
                .coinCost(100)
                .requestFingerprint("fp-integration")
                .build();
        report.markGenerating();
        report.markCompleted("요약", null, "ai", "v1", "v1", "v1", "none");
        reportRepository.save(report);
        conceptRepository.save(WeaknessAnalysisConcept.builder()
                .report(report)
                .conceptId(100L)
                .sortOrder(0)
                .weaknessScore(70)
                .evidenceStrength(new BigDecimal("0.800"))
                .build());
        reportId = report.getAnalysisReportId();
    }

    @Test
    void listReturnsSavedReport() {
        var list = service.getReports(1L);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).analysisReportId()).isEqualTo(reportId);
        assertThat(list.get(0).status()).isEqualTo(AnalysisReportStatus.COMPLETED);
    }

    @Test
    void detailRecordsViewAndReturnsConcept() {
        var detail = service.getReportDetail(1L, reportId);
        assertThat(detail.analysisReportId()).isEqualTo(reportId);
        assertThat(detail.weaknessConcepts()).hasSize(1);
        assertThat(reportRepository.findById(reportId).orElseThrow().getViewCount()).isEqualTo(1);
    }

    @Test
    void deleteSoftDeletesAndHidesFromList() {
        service.deleteReport(1L, reportId);
        assertThat(reportRepository.findById(reportId).orElseThrow().getStatus())
                .isEqualTo(AnalysisReportStatus.DELETED);
        assertThat(service.getReports(1L)).isEmpty();
    }

    @Test
    void detailForOtherUserIsNotFound() {
        CoreMateException ex = assertThrows(CoreMateException.class,
                () -> service.getReportDetail(999L, reportId));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WEAKNESS_REPORT_NOT_FOUND);
    }
}
