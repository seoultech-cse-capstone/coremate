package com.coremate.backend.solve.controller;

import com.coremate.backend.global.config.OpenApiConfig;
import com.coremate.backend.global.response.ApiResponse;
import com.coremate.backend.global.security.CustomUserDetails;
import com.coremate.backend.solve.dto.request.CompleteSolveSessionRequest;
import com.coremate.backend.solve.dto.request.CreateMockSessionRequest;
import com.coremate.backend.solve.dto.request.CreateSolveSessionRequest;
import com.coremate.backend.solve.dto.request.GuestAnswerRequest;
import com.coremate.backend.solve.dto.request.SubmitAnswerRequest;
import com.coremate.backend.solve.dto.response.CompleteSolveSessionResponse;
import com.coremate.backend.solve.dto.response.GuestAnswerResultResponse;
import com.coremate.backend.solve.dto.response.ResumeSessionResponse;
import com.coremate.backend.solve.dto.response.StartSolveSessionResponse;
import com.coremate.backend.solve.dto.response.SubmitAnswerResponse;
import com.coremate.backend.solve.service.SolveSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Solve", description = "문제 풀이 세션 생성, 답안 제출, 완료 처리 API")
@RestController
@RequestMapping("/api/v1/solve-sessions")
@RequiredArgsConstructor
public class SolveSessionController {

    private final SolveSessionService solveSessionService;

    @Operation(
            summary = "개념 문제집 풀이 세션 시작",
            description = "개념 문제집을 시작한다. 비회원은 세션 없이 문제 목록만 반환, 회원은 세션 생성 후 문제 목록 반환."
    )
    @PostMapping("/concept")
    public ApiResponse<StartSolveSessionResponse> startSolveSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateSolveSessionRequest request
    ) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        return ApiResponse.success("풀이 세션 시작 성공",
                solveSessionService.startSolveSession(userId, request));
    }

    @Operation(
            summary = "대분류 챕터 모의고사 풀이 세션 시작 (회원 전용)",
            description = "대분류 범위의 챕터 모의고사를 시작한다. 완료/미완료 개념 구분 없이 해당 범위 내에서 랜덤하게 총 20문제를 제공한다. "
                    + "(만약 총 문제가 20문제가 안되면, 해당 범위 내에 존재하는 모든 문제를 반환)"
    )
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    @PostMapping("/mocks/category-lv1")
    public ApiResponse<StartSolveSessionResponse> startCategoryLv1MockSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateMockSessionRequest request
    ) {
        return ApiResponse.success("대분류 챕터 모의고사 시작 성공",
                solveSessionService.startCategoryLv1MockSession(userDetails.getUserId(), request.targetId()));
    }

    @Operation(
            summary = "중분류 챕터 모의고사 풀이 세션 시작 (회원 전용)",
            description = "중분류 범위의 챕터 모의고사를 시작한다. 완료/미완료 개념 구분 없이 해당 범위 내에서 랜덤하게 총 20문제를 제공한다. "
                    + "(만약 총 문제가 20문제가 안되면, 해당 범위 내에 존재하는 모든 문제를 반환)"
    )
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    @PostMapping("/mocks/category-lv2")
    public ApiResponse<StartSolveSessionResponse> startCategoryLv2MockSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateMockSessionRequest request
    ) {
        return ApiResponse.success("중분류 챕터 모의고사 시작 성공",
                solveSessionService.startCategoryLv2MockSession(userDetails.getUserId(), request.targetId()));
    }

    @Operation(
            summary = "과목 모의고사 풀이 세션 시작 (회원 전용)",
            description = "과목 범위의 모의고사를 시작한다. 완료/미완료 개념 구분 없이 해당 범위 내에서 랜덤하게 총 30문제를 제공한다. "
                    + "(만약 총 문제가 30문제가 안되면, 해당 범위 내에 존재하는 모든 문제를 반환)"
    )
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    @PostMapping("/mocks/subject")
    public ApiResponse<StartSolveSessionResponse> startSubjectMockSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateMockSessionRequest request
    ) {
        return ApiResponse.success("과목 모의고사 시작 성공",
                solveSessionService.startSubjectMockSession(userDetails.getUserId(), request.targetId()));
    }

    @Operation(summary = "진행 중인 세션 재진입 (회원 전용)", description = "중단한 풀이 세션으로 재진입한다. 전체 문제 목록과 이미 제출한 답안을 반환한다.")
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    @GetMapping("/in-progress")
    public ApiResponse<ResumeSessionResponse> resumeSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "개념 ID", example = "1001")
            @RequestParam Long conceptId
    ) {
        return ApiResponse.success("풀이 세션 재진입 성공",
                solveSessionService.resumeSession(userDetails.getUserId(), conceptId));
    }

    @Operation(summary = "문제별 답안 제출 (회원 전용)", description = "회원의 선택 답안을 제출하고 DB에 저장한다.")
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    @PostMapping("/{solveSessionId}/answers")
    public ApiResponse<SubmitAnswerResponse> submitAnswer(
            @Parameter(description = "풀이 세션 ID", example = "9001")
            @PathVariable Long solveSessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SubmitAnswerRequest request
    ) {
        return ApiResponse.success("답안 제출 성공",
                solveSessionService.submitAnswer(solveSessionId, userDetails.getUserId(), request));
    }

    @Operation(summary = "풀이 세션 완료 (회원 전용)", description = "모든 문제를 제출한 뒤 세션을 완료 처리하고 결과를 반환한다.")
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    @PostMapping("/{solveSessionId}/complete")
    public ApiResponse<CompleteSolveSessionResponse> completeSolveSession(
            @Parameter(description = "풀이 세션 ID", example = "9001")
            @PathVariable Long solveSessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CompleteSolveSessionRequest request
    ) {
        return ApiResponse.success("풀이 세션 완료 성공",
                solveSessionService.completeSolveSession(solveSessionId, userDetails.getUserId(), request));
    }

    @Operation(summary = "풀이 세션 완료 (비회원 전용)", description = "비회원이 개념 문제집의 모든 답안을 한 번에 제출하고 문제별 정오답과 해설을 확인한다. DB에 저장되지 않는다.")
    @PostMapping("/guest/complete")
    public ApiResponse<List<GuestAnswerResultResponse>> checkGuestAnswers(
            @Valid @RequestBody GuestAnswerRequest request
    ) {
        return ApiResponse.success("답안 확인 성공",
                solveSessionService.checkGuestAnswers(request));
    }
}
