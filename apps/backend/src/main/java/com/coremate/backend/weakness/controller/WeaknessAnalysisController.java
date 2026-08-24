package com.coremate.backend.weakness.controller;

import com.coremate.backend.global.config.OpenApiConfig;
import com.coremate.backend.global.response.ApiResponse;
import com.coremate.backend.global.security.CustomUserDetails;
import com.coremate.backend.weakness.dto.request.CreateWeaknessAnalysisReportRequest;
import com.coremate.backend.weakness.dto.response.EligibleSolveSessionResponse;
import com.coremate.backend.weakness.dto.response.WeaknessAnalysisCostResponse;
import com.coremate.backend.weakness.dto.response.WeaknessAnalysisReportDetailResponse;
import com.coremate.backend.weakness.dto.response.WeaknessAnalysisReportSummaryResponse;
import com.coremate.backend.weakness.service.WeaknessAnalysisService;
import com.coremate.backend.weakness.service.WeaknessEligibilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 약점 분석 API. 대상 후보 조회 · 분석표 생성(검증→스코어링→AI 해석→교차검증→저장) ·
 * 목록/상세/삭제를 제공한다.
 */
@Tag(name = "Weakness", description = "사용자 약점 분석 대상 조회, 분석표 생성, 목록/상세 조회 API")
@RestController
@RequestMapping("/api/v1/weakness")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class WeaknessAnalysisController {

    private final WeaknessEligibilityService eligibilityService;
    private final WeaknessAnalysisService analysisService;

    @Operation(summary = "약점 분석 대상 완료 모의고사 후보 조회",
            description = "분석표 생성에 사용할 수 있는 완료된 모의고사 회차와 회차별 오답/오래걸림/힌트 수, 선택 가능 여부를 조회한다.")
    @GetMapping("/eligible-solve-sessions")
    public ApiResponse<List<EligibleSolveSessionResponse>> getEligibleSolveSessions(
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Parameter(description = "과목 ID", example = "1")
            @RequestParam(required = false) Long subjectId,

            @Parameter(description = "대분류 ID", example = "20")
            @RequestParam(required = false) Long categoryLv1Id,

            @Parameter(description = "중분류 ID", example = "101")
            @RequestParam(required = false) Long categoryLv2Id,

            @Parameter(description = "개념 ID", example = "1002")
            @RequestParam(required = false) Long conceptId
    ) {
        List<EligibleSolveSessionResponse> data = eligibilityService.getEligibleSessions(
                userDetails.getUserId(), subjectId, categoryLv1Id, categoryLv2Id, conceptId);
        return ApiResponse.success("약점 분석 대상 후보 조회 성공", data);
    }

    @Operation(summary = "약점 분석표 생성 비용 조회",
            description = "분석표 1건 생성에 필요한 코인을 조회한다. 생성 요청과 같은 정책 조회를 쓰므로 "
                    + "여기서 안내한 값과 실제 차감액이 어긋나지 않는다. 생성 확인 화면(SCR-WEAK-03)이 사용한다.")
    @GetMapping("/analysis-cost")
    public ApiResponse<WeaknessAnalysisCostResponse> getAnalysisCost(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.success("약점 분석표 생성 비용 조회 성공",
                new WeaknessAnalysisCostResponse(analysisService.getCreateCoinCost()));
    }

    @Operation(summary = "약점 분석표 생성",
            description = "완료 모의고사 1~5개(같은 과목)를 선택해 검증·스코어링 후 AI 해석·교차검증까지 수행하고 분석표를 저장한다. 코인이 차감된다.")
    @PostMapping("/reports")
    public ApiResponse<WeaknessAnalysisReportDetailResponse> createWeaknessAnalysisReport(
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Valid @RequestBody CreateWeaknessAnalysisReportRequest request
    ) {
        WeaknessAnalysisReportDetailResponse data = analysisService.createReport(userDetails.getUserId(), request);
        return ApiResponse.success("약점 분석표 생성 성공", data);
    }

    @Operation(summary = "생성된 약점 분석표 목록 조회",
            description = "사용자가 생성한(삭제되지 않은) 약점 분석표 목록을 최신순으로 조회한다.")
    @GetMapping("/reports")
    public ApiResponse<List<WeaknessAnalysisReportSummaryResponse>> getWeaknessAnalysisReports(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<WeaknessAnalysisReportSummaryResponse> data = analysisService.getReports(userDetails.getUserId());
        return ApiResponse.success("약점 분석표 목록 조회 성공", data);
    }

    @Operation(summary = "약점 분석표 상세 조회",
            description = "분석표 본문, 약점 개념 목록, 근거 로그를 조회한다. 재열람은 코인이 차감되지 않는다.")
    @GetMapping("/reports/{analysisReportId}")
    public ApiResponse<WeaknessAnalysisReportDetailResponse> getWeaknessAnalysisReportDetail(
            @Parameter(description = "약점 분석표 ID", example = "20001")
            @PathVariable Long analysisReportId,

            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        WeaknessAnalysisReportDetailResponse data = analysisService.getReportDetail(
                userDetails.getUserId(), analysisReportId);
        return ApiResponse.success("약점 분석표 상세 조회 성공", data);
    }

    @Operation(summary = "약점 분석표 삭제",
            description = "분석표를 실제 삭제하지 않고 상태를 DELETED로 변경한다.")
    @DeleteMapping("/reports/{analysisReportId}")
    public ApiResponse<WeaknessAnalysisReportSummaryResponse> deleteWeaknessAnalysisReport(
            @Parameter(description = "약점 분석표 ID", example = "20001")
            @PathVariable Long analysisReportId,

            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        WeaknessAnalysisReportSummaryResponse data = analysisService.deleteReport(
                userDetails.getUserId(), analysisReportId);
        return ApiResponse.success("약점 분석표 삭제 성공", data);
    }
}
