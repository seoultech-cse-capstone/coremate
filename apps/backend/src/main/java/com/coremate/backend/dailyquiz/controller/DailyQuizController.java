package com.coremate.backend.dailyquiz.controller;

import com.coremate.backend.dailyquiz.dto.request.SubmitDailyQuizRequest;
import com.coremate.backend.dailyquiz.dto.response.DailyQuizRewardResponse;
import com.coremate.backend.dailyquiz.dto.response.DailyQuizSetResponse;
import com.coremate.backend.dailyquiz.dto.response.DailyQuizSubmitResponse;
import com.coremate.backend.dailyquiz.service.DailyQuizService;
import com.coremate.backend.global.config.OpenApiConfig;
import com.coremate.backend.global.response.ApiResponse;
import com.coremate.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "DailyQuiz", description = "오늘의 CS 퀴즈 조회, 제출, 보상 결과 API")
@RestController
@RequestMapping("/api/v1/daily-quizzes")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class DailyQuizController {

    private final DailyQuizService dailyQuizService;

    // 오늘 날짜 기준 공통 CS 퀴즈 세트 조회.
    @Operation(summary = "오늘의 CS 퀴즈 조회", description = "오늘 날짜 기준으로 전체 사용자 공통 CS 퀴즈 세트를 조회한다.")
    @GetMapping("/today")
    public ApiResponse<DailyQuizSetResponse> getTodayDailyQuiz(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        DailyQuizSetResponse response = dailyQuizService.getTodayDailyQuiz(userDetails.getUserId());
        return ApiResponse.success("오늘의 CS 퀴즈 조회 성공", response);
    }

    // 오늘의 CS 퀴즈 답안 제출 후 출석과 보상 결과를 함께 반환.
    @Operation(summary = "오늘의 CS 퀴즈 제출", description = "오늘의 CS 퀴즈 답안을 제출하고 출석 처리 및 보상 결과를 반환한다.")
    @PostMapping("/{dailyQuizSetId}/submit")
    public ApiResponse<DailyQuizSubmitResponse> submitDailyQuiz(
            @Parameter(description = "오늘의 CS 퀴즈 세트 ID", example = "2026051101")
            @PathVariable Long dailyQuizSetId,

            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Valid @RequestBody SubmitDailyQuizRequest request
    ) {
        DailyQuizSubmitResponse response = dailyQuizService.submitDailyQuiz(
                userDetails.getUserId(),
                dailyQuizSetId,
                request
        );
        return ApiResponse.success("오늘의 CS 퀴즈 제출 성공", response);
    }

    // 오늘의 CS 퀴즈 완료 후 지급된 보상 조회.
    @Operation(summary = "오늘의 CS 퀴즈 보상 조회", description = "오늘의 CS 퀴즈 완료 후 지급된 XP, 코인, 랜덤박스 보상 정보를 조회한다.")
    @GetMapping("/today/reward")
    public ApiResponse<DailyQuizRewardResponse> getTodayDailyQuizReward(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        DailyQuizRewardResponse response = dailyQuizService.getTodayDailyQuizReward(userDetails.getUserId());
        return ApiResponse.success("오늘의 CS 퀴즈 보상 조회 성공", response);
    }
}
