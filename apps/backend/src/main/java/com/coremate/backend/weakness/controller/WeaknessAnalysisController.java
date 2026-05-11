package com.coremate.backend.weakness.controller;

import com.coremate.backend.global.response.ApiResponse;
import com.coremate.backend.solve.dto.type.SolveSessionType;
import com.coremate.backend.weakness.dto.request.CreateWeaknessAnalysisReportRequest;
import com.coremate.backend.weakness.dto.response.EligibleSolveSessionResponse;
import com.coremate.backend.weakness.dto.response.WeaknessAnalysisReportDetailResponse;
import com.coremate.backend.weakness.dto.response.WeaknessAnalysisReportFileResponse;
import com.coremate.backend.weakness.dto.response.WeaknessAnalysisReportSummaryResponse;
import com.coremate.backend.weakness.dto.response.WeaknessConceptResponse;
import com.coremate.backend.weakness.dto.response.WeaknessEvidenceLogResponse;
import com.coremate.backend.weakness.dto.type.AnalysisReportStatus;
import com.coremate.backend.weakness.dto.type.AnalysisScopeType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Weakness", description = "사용자 약점 분석 대상 조회, 분석표 생성, 목록/상세/파일 조회 API")
@RestController
@RequestMapping("/api/v1/weakness")
public class WeaknessAnalysisController {

    @Operation(summary = "약점 분석 대상 완료 문제집 후보 조회", description = "사용자가 분석표 생성에 사용할 수 있는 완료된 문제집/모의고사 회차를 조회한다.")
    @GetMapping("/eligible-solve-sessions")
    public ApiResponse<List<EligibleSolveSessionResponse>> getEligibleSolveSessions(
            @Parameter(description = "임시 사용자 ID", example = "1") @RequestParam(defaultValue = "1") Long userId,
            @Parameter(description = "과목 ID", example = "1") @RequestParam(required = false) Long subjectId,
            @Parameter(description = "대분류 ID", example = "20") @RequestParam(required = false) Long categoryLv1Id,
            @Parameter(description = "중분류 ID", example = "101") @RequestParam(required = false) Long categoryLv2Id,
            @Parameter(description = "개념 ID", example = "1002") @RequestParam(required = false) Long conceptId
    ) {
        return ApiResponse.success("약점 분석 대상 후보 조회 성공", sampleEligibleSessions());
    }

    @Operation(summary = "약점 분석표 생성 요청", description = "완료 문제집 1~10개를 선택해 AI 약점 분석표 생성을 요청한다. 현재는 OpenAI 호출 없이 더미 완료 응답을 반환한다.")
    @PostMapping("/reports")
    public ApiResponse<WeaknessAnalysisReportDetailResponse> createWeaknessAnalysisReport(
            @Parameter(description = "임시 사용자 ID", example = "1") @RequestParam(defaultValue = "1") Long userId,
            @Valid @RequestBody CreateWeaknessAnalysisReportRequest request
    ) {
        WeaknessAnalysisReportDetailResponse response = sampleReportDetail(
                20001L,
                userId,
                request.title() == null || request.title().isBlank() ? "자료구조 이진 트리 약점 분석표" : request.title(),
                request.analysisScopeType()
        );
        return ApiResponse.success("약점 분석표 생성 요청 성공", response);
    }

    @Operation(summary = "생성된 약점 분석표 목록 조회", description = "사용자가 생성한 약점 분석표 목록을 조회한다.")
    @GetMapping("/reports")
    public ApiResponse<List<WeaknessAnalysisReportSummaryResponse>> getWeaknessAnalysisReports(
            @Parameter(description = "임시 사용자 ID", example = "1") @RequestParam(defaultValue = "1") Long userId,
            @Parameter(description = "분석 범위 필터", example = "CATEGORY_LV2") @RequestParam(required = false) AnalysisScopeType analysisScopeType,
            @Parameter(description = "상태 필터", example = "COMPLETED") @RequestParam(required = false) AnalysisReportStatus status
    ) {
        return ApiResponse.success("약점 분석표 목록 조회 성공", List.of(
                new WeaknessAnalysisReportSummaryResponse(20001L, "자료구조 이진 트리 약점 분석표", AnalysisScopeType.CATEGORY_LV2, "자료구조 > 트리 > 이진 트리", AnalysisReportStatus.COMPLETED, 2, 10, 4, 100, LocalDateTime.now().minusHours(1), true),
                new WeaknessAnalysisReportSummaryResponse(20002L, "자료구조 트리 약점 분석표", AnalysisScopeType.CATEGORY_LV1, "자료구조 > 트리", AnalysisReportStatus.COMPLETED, 3, 15, 5, 100, LocalDateTime.now().minusDays(3), true)
        ));
    }

    @Operation(summary = "약점 분석표 상세 조회", description = "분석표 본문, 약점 개념 목록, 근거 로그, PDF/뷰어 정보를 조회한다.")
    @GetMapping("/reports/{analysisReportId}")
    public ApiResponse<WeaknessAnalysisReportDetailResponse> getWeaknessAnalysisReportDetail(
            @Parameter(description = "약점 분석표 ID", example = "20001") @PathVariable Long analysisReportId,
            @Parameter(description = "임시 사용자 ID", example = "1") @RequestParam(defaultValue = "1") Long userId
    ) {
        return ApiResponse.success("약점 분석표 상세 조회 성공", sampleReportDetail(analysisReportId, userId, "자료구조 이진 트리 약점 분석표", AnalysisScopeType.CATEGORY_LV2));
    }

    @Operation(summary = "약점 분석표 파일 정보 조회", description = "PDF 다운로드 URL, 앱 내 뷰어 타입, 저장 경로 정보를 조회한다.")
    @GetMapping("/reports/{analysisReportId}/file")
    public ApiResponse<WeaknessAnalysisReportFileResponse> getWeaknessAnalysisReportFile(
            @Parameter(description = "약점 분석표 ID", example = "20001") @PathVariable Long analysisReportId
    ) {
        return ApiResponse.success("약점 분석표 파일 정보 조회 성공", sampleFile(analysisReportId));
    }

    @Operation(summary = "약점 분석표 삭제", description = "분석표를 실제 삭제하지 않고 상태를 DELETED로 변경하는 API 계약이다.")
    @DeleteMapping("/reports/{analysisReportId}")
    public ApiResponse<WeaknessAnalysisReportSummaryResponse> deleteWeaknessAnalysisReport(
            @Parameter(description = "약점 분석표 ID", example = "20001") @PathVariable Long analysisReportId
    ) {
        WeaknessAnalysisReportSummaryResponse response = new WeaknessAnalysisReportSummaryResponse(
                analysisReportId,
                "자료구조 이진 트리 약점 분석표",
                AnalysisScopeType.CATEGORY_LV2,
                "자료구조 > 트리 > 이진 트리",
                AnalysisReportStatus.DELETED,
                2,
                10,
                4,
                100,
                LocalDateTime.now().minusHours(1),
                false
        );
        return ApiResponse.success("약점 분석표 삭제 성공", response);
    }

    private WeaknessAnalysisReportDetailResponse sampleReportDetail(Long reportId, Long userId, String title, AnalysisScopeType scopeType) {
        return new WeaknessAnalysisReportDetailResponse(
                reportId,
                userId,
                title,
                scopeType,
                "자료구조 > 트리 > 이진 트리",
                "완전 이진 트리와 포화 이진 트리의 구분에서 반복적인 오답과 긴 풀이 시간이 나타났습니다.",
                "## 약점 요약\n완전 이진 트리의 핵심 조건은 마지막 레벨의 왼쪽 채움입니다. 포화 이진 트리와 혼동하지 않도록 두 개념의 기준을 분리해 복습해야 합니다.",
                "MARKDOWN",
                2,
                10,
                4,
                2,
                3,
                100,
                AnalysisReportStatus.COMPLETED,
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),
                sampleEligibleSessions(),
                sampleWeaknessConcepts(),
                sampleFile(reportId)
        );
    }

    private List<EligibleSolveSessionResponse> sampleEligibleSessions() {
        return List.of(
                new EligibleSolveSessionResponse(9001L, "완전 이진 트리 개념 문제집", SolveSessionType.CONCEPT_PRACTICE, 1L, "자료구조", 20L, "트리", 101L, "이진 트리", 1002L, "완전 이진 트리", 5, 2, 1, 1, LocalDateTime.now().minusDays(1), true, "틀린 문항이 2개 이상이라 분석 추천"),
                new EligibleSolveSessionResponse(9002L, "이진 트리 챕터 모의고사", SolveSessionType.CATEGORY_LV2_MOCK, 1L, "자료구조", 20L, "트리", 101L, "이진 트리", null, null, 10, 4, 2, 3, LocalDateTime.now().minusDays(2), true, "오답과 힌트 사용 기록이 있어 분석 추천")
        );
    }

    private List<WeaknessConceptResponse> sampleWeaknessConcepts() {
        List<WeaknessEvidenceLogResponse> evidenceLogs = List.of(
                new WeaknessEvidenceLogResponse(40001L, 10001L, 5001L, "완전 이진 트리에 대한 설명으로 가장 적절한 것은?", "WRONG", 80, 45, 1, "A", "B", false),
                new WeaknessEvidenceLogResponse(40002L, 10003L, 5003L, "포화 이진 트리와 완전 이진 트리의 차이로 적절한 것은?", "LONG_TIME", 95, 60, 0, "B", "B", true),
                new WeaknessEvidenceLogResponse(40003L, 10004L, 5004L, "힙이 배열로 표현되기 쉬운 이유는?", "HINT_USED", 70, 50, 2, "C", "C", true)
        );
        return List.of(
                new WeaknessConceptResponse(
                        30001L,
                        1002L,
                        "완전 이진 트리",
                        4,
                        "완전 이진 트리의 마지막 레벨 조건을 포화 이진 트리 조건과 혼동한 풀이 기록이 반복되었습니다.",
                        "오답 1개, 오래 머문 문제 1개, 힌트 사용 문제 1개가 이 개념과 연결됩니다.",
                        "완전 이진 트리는 모든 레벨이 완전히 꽉 차야 하는 개념이 아닙니다. 마지막 레벨을 제외한 레벨은 모두 채워져야 하며, 마지막 레벨은 왼쪽부터 빈틈없이 채워지면 됩니다. 이 조건 때문에 힙처럼 배열 인덱스로 부모와 자식 관계를 계산하기 쉬운 구조가 됩니다.",
                        "포화 이진 트리, 완전 이진 트리, 편향 이진 트리의 조건을 표로 나누어 복습하고, 그림을 보고 어떤 트리인지 판별하는 문제를 다시 풀어보세요.",
                        evidenceLogs
                )
        );
    }

    private WeaknessAnalysisReportFileResponse sampleFile(Long reportId) {
        return new WeaknessAnalysisReportFileResponse(
                reportId,
                "PDF",
                "https://cdn.coremate.app/reports/weakness/" + reportId + ".pdf",
                "reports/weakness/" + reportId + ".pdf",
                true
        );
    }
}
