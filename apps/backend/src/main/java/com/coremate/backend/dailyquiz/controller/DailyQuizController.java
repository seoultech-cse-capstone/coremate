package com.coremate.backend.dailyquiz.controller;

import com.coremate.backend.dailyquiz.dto.request.SubmitDailyQuizRequest;
import com.coremate.backend.dailyquiz.dto.response.DailyQuizQuestionResponse;
import com.coremate.backend.dailyquiz.dto.response.DailyQuizRewardResponse;
import com.coremate.backend.dailyquiz.dto.response.DailyQuizSetResponse;
import com.coremate.backend.dailyquiz.dto.response.DailyQuizSubmitResponse;
import com.coremate.backend.global.config.OpenApiConfig;
import com.coremate.backend.global.response.ApiResponse;
import com.coremate.backend.problemset.dto.response.QuestionChoiceResponse;
import com.coremate.backend.problemset.dto.type.QuestionFormat;
import com.coremate.backend.problemset.dto.type.QuestionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "DailyQuiz", description = "오늘의 CS 퀴즈 조회, 제출, 보상 결과 API")
@RestController
@RequestMapping("/api/v1/daily-quizzes")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class DailyQuizController {

    // 오늘 날짜 기준 공통 CS 퀴즈 세트 조회
    @Operation(summary = "오늘의 CS 퀴즈 조회", description = "오늘 날짜 기준으로 전체 사용자 공통 CS 퀴즈 세트를 조회한다.")
    @GetMapping("/today")
    public ApiResponse<DailyQuizSetResponse> getTodayDailyQuiz(
            @Parameter(description = "임시 사용자 ID. 오늘 제출 여부 확인용", example = "1")
            @RequestParam(required = false) Long userId
    ) {
        // DB 연동 전 퀴즈 세트 응답 구조 확인용 더미 데이터
        DailyQuizSetResponse response = new DailyQuizSetResponse(
                2026051101L,
                LocalDate.now(),
                sampleQuestions().size(),
                false,
                sampleQuestions()
        );

        return ApiResponse.success("오늘의 CS 퀴즈 조회 성공", response);
    }

    // 오늘의 CS 퀴즈 답안 제출 및 보상 결과 반환
    @Operation(summary = "오늘의 CS 퀴즈 제출", description = "오늘의 CS 퀴즈 답안을 제출하고 출석 처리 및 보상 결과를 반환한다.")
    @PostMapping("/{dailyQuizSetId}/submit")
    public ApiResponse<DailyQuizSubmitResponse> submitDailyQuiz(
            @Parameter(description = "오늘의 CS 퀴즈 세트 ID", example = "2026051101")
            @PathVariable Long dailyQuizSetId,

            @Parameter(description = "임시 사용자 ID", example = "1")
            @RequestParam(defaultValue = "1") Long userId,

            @Valid @RequestBody SubmitDailyQuizRequest request
    ) {
        // 출석 보상 결과 응답용 더미 데이터
        DailyQuizRewardResponse reward = new DailyQuizRewardResponse(
                10,
                5,
                1L,
                "하급 랜덤박스",
                true,
                true
        );

        // 제출 결과 응답 구조 확인용 더미 데이터
        DailyQuizSubmitResponse response = new DailyQuizSubmitResponse(
                9101L,
                dailyQuizSetId,
                LocalDate.now(),
                request.answers().size(),
                Math.max(0, request.answers().size() - 1),
                request.answers().isEmpty() ? 0 : 1,
                request.totalTimeSpentSec() == null ? 180 : request.totalTimeSpentSec(),
                true,
                reward
        );

        return ApiResponse.success("오늘의 CS 퀴즈 제출 성공", response);
    }

    // 오늘의 CS 퀴즈 완료 후 지급 보상 조회
    @Operation(summary = "오늘의 CS 퀴즈 보상 조회", description = "오늘의 CS 퀴즈 완료 후 지급된 XP, 코인, 랜덤박스 보상 정보를 조회한다.")
    @GetMapping("/today/reward")
    public ApiResponse<DailyQuizRewardResponse> getTodayDailyQuizReward(
            @Parameter(description = "임시 사용자 ID", example = "1")
            @RequestParam(defaultValue = "1") Long userId
    ) {
        // DB 연동 전 보상 응답 구조 확인용 더미 데이터
        return ApiResponse.success("오늘의 CS 퀴즈 보상 조회 성공", new DailyQuizRewardResponse(
                10,
                5,
                1L,
                "하급 랜덤박스",
                true,
                true
        ));
    }

    // 오늘의 CS 퀴즈 문제 목록 더미 데이터
    private List<DailyQuizQuestionResponse> sampleQuestions() {
        // 객관식 4지선다 선택지 더미 데이터
        List<QuestionChoiceResponse> choices = List.of(
                new QuestionChoiceResponse("A", "스택"),
                new QuestionChoiceResponse("B", "큐"),
                new QuestionChoiceResponse("C", "완전 이진 트리"),
                new QuestionChoiceResponse("D", "해시 테이블")
        );

        // 과목별 퀴즈 문제 응답 구조 확인용 더미 데이터
        return List.of(
                new DailyQuizQuestionResponse(5001L, 1L, "자료구조", 1002L, "완전 이진 트리", 1, QuestionFormat.MCQ_4, QuestionType.DEFINITION, "힙 구현에 자주 사용되는 트리 형태는 무엇인가?", choices, 2, 45, true),
                new DailyQuizQuestionResponse(6001L, 3L, "운영체제", 2001L, "프로세스", 2, QuestionFormat.MCQ_4, QuestionType.DEFINITION, "실행 중인 프로그램을 의미하는 운영체제 개념은 무엇인가?", choices, 1, 30, false)
        );
    }
}