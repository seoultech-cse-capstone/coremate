package com.coremate.backend.weakness.controller;

import com.coremate.backend.global.config.OpenApiConfig;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.global.response.ApiResponse;
import com.coremate.backend.global.security.CustomUserDetails;
import com.coremate.backend.weakness.dto.request.CreateWeaknessAnalysisReportRequest;
import com.coremate.backend.weakness.dto.response.EligibleSolveSessionResponse;
import com.coremate.backend.weakness.dto.response.WeaknessAnalysisReportDetailResponse;
import com.coremate.backend.weakness.dto.response.WeaknessAnalysisReportSummaryResponse;
import com.coremate.backend.weakness.dto.type.AnalysisReportStatus;
import com.coremate.backend.weakness.dto.type.AnalysisScopeType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
 * 사용자 약점 분석 API 계약(엔드포인트 · 요청/응답 DTO 정의).
 *
 * <p>Phase 0 기준으로 스키마와 계약만 확정된 상태다. 실제 선택/검증/집계/AI 연동 로직은
 * Phase 1(백엔드 결정론 코어) 이후 서비스 계층으로 구현하며, 현재 모든 엔드포인트는
 * 미구현({@link ErrorCode#NOT_IMPLEMENTED})을 반환한다.
 *
 * <p>테스트 데이터는 코드에 하드코딩하지 않고 {@code scripts/db/weakness_dummy_seed.sql}로
 * MySQL에 주입한 뒤, Phase 1 로직이 그 실제 데이터를 읽도록 한다.
 */
@Tag(name = "Weakness", description = "사용자 약점 분석 대상 조회, 분석표 생성, 목록/상세 조회 API")
@RestController
@RequestMapping("/api/v1/weakness")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class WeaknessAnalysisController {

    @Operation(summary = "약점 분석 대상 완료 모의고사 후보 조회",
            description = "분석표 생성에 사용할 수 있는 완료된 모의고사 회차를 조회한다. (Phase 1 구현 예정)")
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
        throw new CoreMateException(ErrorCode.NOT_IMPLEMENTED);
    }

    @Operation(summary = "약점 분석표 생성 요청",
            description = "완료 모의고사 1~5개(같은 과목)를 선택해 AI 약점 분석표 생성을 요청한다. (Phase 1~3 구현 예정)")
    @PostMapping("/reports")
    public ApiResponse<WeaknessAnalysisReportDetailResponse> createWeaknessAnalysisReport(
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Valid @RequestBody CreateWeaknessAnalysisReportRequest request
    ) {
        throw new CoreMateException(ErrorCode.NOT_IMPLEMENTED);
    }

    @Operation(summary = "생성된 약점 분석표 목록 조회",
            description = "사용자가 생성한 약점 분석표 목록을 조회한다. (Phase 1 구현 예정)")
    @GetMapping("/reports")
    public ApiResponse<List<WeaknessAnalysisReportSummaryResponse>> getWeaknessAnalysisReports(
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Parameter(description = "분석 범위 필터", example = "CATEGORY_LV2")
            @RequestParam(required = false) AnalysisScopeType analysisScopeType,

            @Parameter(description = "상태 필터", example = "COMPLETED")
            @RequestParam(required = false) AnalysisReportStatus status
    ) {
        throw new CoreMateException(ErrorCode.NOT_IMPLEMENTED);
    }

    @Operation(summary = "약점 분석표 상세 조회",
            description = "분석표 본문, 약점 개념 목록, 근거 로그를 조회한다. (Phase 1 구현 예정)")
    @GetMapping("/reports/{analysisReportId}")
    public ApiResponse<WeaknessAnalysisReportDetailResponse> getWeaknessAnalysisReportDetail(
            @Parameter(description = "약점 분석표 ID", example = "20001")
            @PathVariable Long analysisReportId,

            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        throw new CoreMateException(ErrorCode.NOT_IMPLEMENTED);
    }

    @Operation(summary = "약점 분석표 삭제",
            description = "분석표를 실제 삭제하지 않고 상태를 DELETED로 변경한다. (Phase 1 구현 예정)")
    @DeleteMapping("/reports/{analysisReportId}")
    public ApiResponse<WeaknessAnalysisReportSummaryResponse> deleteWeaknessAnalysisReport(
            @Parameter(description = "약점 분석표 ID", example = "20001")
            @PathVariable Long analysisReportId,

            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        throw new CoreMateException(ErrorCode.NOT_IMPLEMENTED);
    }
}
