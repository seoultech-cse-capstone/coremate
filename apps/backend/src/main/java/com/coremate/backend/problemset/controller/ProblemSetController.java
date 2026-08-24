package com.coremate.backend.problemset.controller;

import com.coremate.backend.global.config.OpenApiConfig;
import com.coremate.backend.global.response.ApiResponse;
import com.coremate.backend.global.security.CustomUserDetails;
import com.coremate.backend.problemset.dto.response.CompletedProblemSetDetailResponse;
import com.coremate.backend.problemset.dto.response.CompletedProblemSetSummaryResponse;
import com.coremate.backend.problemset.service.CompletedProblemSetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "ProblemSet", description = "완료된 문제집/모의고사 회차 조회 API")
@RestController
@RequestMapping("/api/v1/problem-sets")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@RequiredArgsConstructor
public class ProblemSetController {

    private final CompletedProblemSetService completedProblemSetService;

    @Operation(
            summary = "완료된 문제집 회차 목록 조회 (회원 전용)",
            description = "회원 사용자가 완료한 개념 문제집/모의고사 회차 목록을 반환한다. "
                    + "과목/대분류/중분류 ID로 필터링 가능하며, 각 필터는 생략 시 전체 조회로 동작한다. "
                    + "완료 시간 내림차순으로 정렬된다."
    )
    @GetMapping("/completed")
    public ApiResponse<List<CompletedProblemSetSummaryResponse>> getCompletedProblemSets(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "과목 ID 필터", example = "1")
            @RequestParam(required = false) Long subjectId,
            @Parameter(description = "대분류 ID 필터", example = "20")
            @RequestParam(required = false) Long categoryLv1Id,
            @Parameter(description = "중분류 ID 필터", example = "101")
            @RequestParam(required = false) Long categoryLv2Id
    ) {
        return ApiResponse.success("완료된 문제집 목록 조회 성공",
                completedProblemSetService.getCompletedProblemSets(
                        userDetails.getUserId(), subjectId, categoryLv1Id, categoryLv2Id));
    }

    @Operation(
            summary = "완료된 문제집 회차 상세 조회 (회원 전용)",
            description = "완료된 회차의 문제별 사용자 선택 선지, 정답, 정오답, 해설, 소요 시간, 힌트 사용, 답 변경, 즐겨찾기 상태를 반환한다. 본인 회차만 조회 가능."
    )
    @GetMapping("/completed/{solveSessionId}")
    public ApiResponse<CompletedProblemSetDetailResponse> getCompletedProblemSetDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "풀이 세션 ID", example = "9001")
            @PathVariable Long solveSessionId
    ) {
        return ApiResponse.success("완료된 문제집 상세 조회 성공",
                completedProblemSetService.getCompletedProblemSetDetail(
                        userDetails.getUserId(), solveSessionId));
    }
}
