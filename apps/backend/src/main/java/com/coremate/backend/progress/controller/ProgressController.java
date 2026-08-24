package com.coremate.backend.progress.controller;

import com.coremate.backend.global.config.OpenApiConfig;
import com.coremate.backend.global.response.ApiResponse;
import com.coremate.backend.global.security.CustomUserDetails;
import com.coremate.backend.problemset.dto.type.ProblemSetType;
import com.coremate.backend.progress.dto.response.ConceptCountSummaryResponse;
import com.coremate.backend.progress.dto.response.ProblemSetProgressResponse;
import com.coremate.backend.progress.dto.response.SubjectProgressResponse;
import com.coremate.backend.progress.service.ProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Progress", description = "문제집 완료 상태, 개념 완료 상태, 과목별 진행률 API")
@RestController
@RequestMapping("/api/v1/progress")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class ProgressController {

    private final ProgressService progressService;

    // 복습탭 개념 카운팅(총 개념 / 풀었어요 / 아직 못풀었어요) 조회
    @Operation(summary = "복습탭 개념 카운팅 조회",
            description = "복습탭에 표시할 총 개념 / 풀었어요 / 아직 못풀었어요 수를 전역(과목 무관) 기준으로 조회한다. "
                    + "풀었어요는 COMPLETED 상태의 개념 문제집 세션이 있는 개념만 집계하며, "
                    + "아직 못풀었어요는 전체에서 풀었어요를 뺀 값(미풀이 + 진행중 포함)이다.")
    @GetMapping("/concepts/summary")
    public ApiResponse<ConceptCountSummaryResponse> getConceptCountSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.success("복습탭 개념 카운팅 조회 성공",
                progressService.getConceptCountSummary(userDetails.getUserId()));
    }

    // 문제집별 완료/미완료 상태 조회
    @Operation(summary = "문제집별 완료 상태 조회", description = "문제집 목록 오른쪽 완료/미완료 표시와 완료 횟수 표시를 위한 API")
    @GetMapping("/problem-sets")
    public ApiResponse<List<ProblemSetProgressResponse>> getProblemSetProgress(
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Parameter(description = "문제집 유형", example = "CONCEPT_PRACTICE")
            @RequestParam(required = false) ProblemSetType problemSetType,

            @Parameter(description = "과목 ID", example = "1")
            @RequestParam(required = false) Long subjectId,

            @Parameter(description = "대분류 ID", example = "20")
            @RequestParam(required = false) Long categoryLv1Id,

            @Parameter(description = "중분류 ID", example = "101")
            @RequestParam(required = false) Long categoryLv2Id
    ) {
        return ApiResponse.success("문제집별 완료 상태 조회 성공",
                progressService.getProblemSetProgress(
                        userDetails.getUserId(), problemSetType, subjectId, categoryLv1Id, categoryLv2Id));
    }

    // 과목별 학습 진행률 및 스탯 조회
    @Operation(summary = "과목별 학습 진행률 조회", description = "마이페이지의 과목별 학습 달성률과 육각형 스탯에 사용할 데이터를 조회한다.")
    @GetMapping("/subjects")
    public ApiResponse<List<SubjectProgressResponse>> getSubjectProgress(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.success("과목별 학습 진행률 조회 성공",
                progressService.getSubjectProgress(userDetails.getUserId()));
    }
}