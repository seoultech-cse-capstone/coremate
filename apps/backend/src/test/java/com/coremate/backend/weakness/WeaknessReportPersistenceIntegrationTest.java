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
                .questionCount(3)
                .wrongCount(2)
                .longTimeCount(0)
                .hintUsedCount(1)
                .build());
        // 개념 수(2)와 오답 수(4)를 다르게 둬야 목록의 "발견한 약점"이 어느 값을 세는지 구분된다.
        conceptRepository.save(WeaknessAnalysisConcept.builder()
                .report(report)
                .conceptId(101L)
                .sortOrder(1)
                .weaknessScore(55)
                .evidenceStrength(new BigDecimal("0.600"))
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

    /**
     * 목록의 weaknessConceptCount 는 "약점으로 잡힌 개념 수"다. 예전엔 화면이 오답 수를
     * "발견한 약점"으로 보여줘 라벨과 값이 어긋났으므로, 두 값이 다른 상태로 못 박아 둔다.
     */
    @Test
    void listCountsWeaknessConceptsNotWrongAnswers() {
        var summary = service.getReports(1L).get(0);
        assertThat(summary.weaknessConceptCount()).isEqualTo(2);
        assertThat(summary.totalWrongCount()).isEqualTo(4);
    }

    @Test
    void detailRecordsViewAndReturnsConcept() {
        var detail = service.getReportDetail(1L, reportId);
        assertThat(detail.analysisReportId()).isEqualTo(reportId);
        assertThat(detail.weaknessConcepts()).hasSize(2);
        assertThat(reportRepository.findById(reportId).orElseThrow().getViewCount()).isEqualTo(1);
    }

    /**
     * 개념별 수치는 생성 때만 계산되던 값이라 저장하지 않으면 재열람 응답의 풀이 요약이 빈다.
     * 저장한 개념은 같은 요약이 나오고, 수치가 없는 예전 분석표는 요약을 비운 채 나머지만 준다.
     */
    @Test
    void detailRestoresStoredCountsAndSummary() {
        var detail = service.getReportDetail(1L, reportId);

        var stored = detail.weaknessConcepts().get(0);
        assertThat(stored.questionCount()).isEqualTo(3);
        assertThat(stored.wrongCount()).isEqualTo(2);
        assertThat(stored.hintUsedCount()).isEqualTo(1);
        assertThat(stored.evidenceSummary()).isEqualTo("오답 2개, 오래 걸린 문제 0개, 힌트 사용 1개");

        var legacy = detail.weaknessConcepts().get(1);
        assertThat(legacy.questionCount()).isZero();
        assertThat(legacy.evidenceSummary()).isNull();
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
