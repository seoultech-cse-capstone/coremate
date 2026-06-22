package com.coremate.backend.solve.controller;

import com.coremate.backend.global.response.ApiResponse;
import com.coremate.backend.problemset.dto.response.QuestionChoiceResponse;
import com.coremate.backend.solve.dto.request.CompleteSolveSessionRequest;
import com.coremate.backend.solve.dto.request.CreateSolveSessionRequest;
import com.coremate.backend.solve.dto.request.SubmitAnswerRequest;
import com.coremate.backend.solve.dto.response.CompleteSolveSessionResponse;
import com.coremate.backend.solve.dto.response.CompletedSolveSessionResponse;
import com.coremate.backend.solve.dto.response.SolveLogReviewResponse;
import com.coremate.backend.solve.dto.response.SolveSessionResponse;
import com.coremate.backend.solve.dto.response.SolveSessionReviewResponse;
import com.coremate.backend.solve.dto.response.SubmitAnswerResponse;
import com.coremate.backend.solve.dto.type.SolveSessionStatus;
import com.coremate.backend.solve.dto.type.SolveSessionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Solve", description = "문제 풀이 세션 생성, 답안 제출, 완료 기록/해설 조회 API")
@RestController
@RequestMapping("/api/v1/solve-sessions")
public class SolveSessionController {

    // 문제집/모의고사 풀이 세션 생성
    @Operation(summary = "풀이 세션 생성", description = "문제집 또는 모의고사를 시작할 때 풀이 회차를 생성한다. 현재는 Swagger용 더미 응답이다.")
    @PostMapping
    public ApiResponse<SolveSessionResponse> createSolveSession(
            @Parameter(description = "임시 사용자 ID. 실제 JWT 적용 전까지 Swagger 테스트용", example = "1")
            @RequestParam(defaultValue = "1") Long userId,

            @Valid @RequestBody CreateSolveSessionRequest request
    ) {

        // DB 연동 전 세션 생성 응답 구조 확인용 더미 데이터
        SolveSessionResponse response = new SolveSessionResponse(
                9001L,
                userId,
                request.sessionType(),
                request.targetId(),
                resolveTitle(request.sessionType()),
                3,
                SolveSessionStatus.IN_PROGRESS,
                LocalDateTime.now(),
                request.questionIds() == null || request.questionIds().isEmpty() ? List.of(5001L, 5002L, 5003L) : request.questionIds()
        );

        return ApiResponse.success("풀이 세션 생성 성공", response);
    }

    // 문제별 답안 제출
    @Operation(summary = "문제별 답안 제출", description = "사용자의 선택 답안, 풀이 시간, 힌트 사용 여부, 답 변경 여부를 제출한다.")
    @PostMapping("/{solveSessionId}/answers")
    public ApiResponse<SubmitAnswerResponse> submitAnswer(
            @Parameter(description = "풀이 세션 ID", example = "9001")
            @PathVariable Long solveSessionId,

            @Valid @RequestBody SubmitAnswerRequest request
    ) {

        // 임시 정답 판별 로직
        boolean isCorrect = "B".equalsIgnoreCase(request.selectedAnswer());

        // DB 연동 전 답안 제출 결과 확인용 더미 데이터
        SubmitAnswerResponse response = new SubmitAnswerResponse(
                10001L,
                request.questionId(),
                request.selectedAnswer(),
                "B",
                isCorrect,
                "완전 이진 트리는 마지막 레벨을 제외한 모든 레벨이 채워지고, 마지막 레벨은 왼쪽부터 연속적으로 채워지는 이진 트리입니다.",
                2,
                false
        );

        return ApiResponse.success("답안 제출 성공", response);
    }

    // 풀이 세션 완료 처리
    @Operation(summary = "풀이 세션 완료", description = "문제집/모의고사 풀이를 완료 처리하고 결과 요약을 반환한다.")
    @PatchMapping("/{solveSessionId}/complete")
    public ApiResponse<CompleteSolveSessionResponse> completeSolveSession(
            @Parameter(description = "풀이 세션 ID", example = "9001")
            @PathVariable Long solveSessionId,

            @Valid @RequestBody CompleteSolveSessionRequest request
    ) {

        // DB 연동 전 완료 결과 요약 확인용 더미 데이터
        CompleteSolveSessionResponse response = new CompleteSolveSessionResponse(
                solveSessionId,
                "완전 이진 트리 개념 문제집",
                SolveSessionType.CONCEPT_PRACTICE,
                3,
                2,
                1,
                request.totalTimeSpentSec() == null ? 420 : request.totalTimeSpentSec(),
                SolveSessionStatus.COMPLETED,
                LocalDateTime.now(),
                20,
                10
        );

        return ApiResponse.success("풀이 세션 완료 성공", response);
    }

    // 완료된 문제집 후보 목록 조회
    @Operation(summary = "완료된 문제집 목록 조회", description = "문제집 페이지와 사용자 약점 분석 페이지에서 완료된 문제집 후보를 조회한다.")
    @GetMapping("/completed")
    public ApiResponse<List<CompletedSolveSessionResponse>> getCompletedSolveSessions(
            @Parameter(description = "임시 사용자 ID", example = "1")
            @RequestParam(defaultValue = "1") Long userId,

            @Parameter(description = "과목 ID 필터", example = "1")
            @RequestParam(required = false) Long subjectId,

            @Parameter(description = "대분류 ID 필터", example = "20")
            @RequestParam(required = false) Long categoryLv1Id,

            @Parameter(description = "중분류 ID 필터", example = "101")
            @RequestParam(required = false) Long categoryLv2Id,

            @Parameter(description = "풀이 유형 필터", example = "CONCEPT_PRACTICE")
            @RequestParam(required = false) SolveSessionType sessionType
    ) {

        // DB 연동 전 완료 문제집 목록 확인용 더미 데이터
        return ApiResponse.success("완료된 문제집 목록 조회 성공", List.of(
                new CompletedSolveSessionResponse(9001L, "완전 이진 트리 개념 문제집", SolveSessionType.CONCEPT_PRACTICE, 1L, "자료구조", 20L, "트리", 101L, "이진 트리", 1002L, "완전 이진 트리", 5, 2, LocalDateTime.now().minusDays(1)),
                new CompletedSolveSessionResponse(9002L, "이진 트리 챕터 모의고사", SolveSessionType.CATEGORY_LV2_MOCK, 1L, "자료구조", 20L, "트리", 101L, "이진 트리", null, null, 10, 4, LocalDateTime.now().minusDays(2))
        ));
    }

    // 완료된 문제집 기록/해설 상세 조회
    @Operation(summary = "완료된 문제집 기록/해설 상세 조회", description = "완료된 문제집을 선택했을 때 내가 푼 문제 목록, 선택 답, 정답, 해설, 소요 시간, 힌트 사용 여부, 답 변경 여부를 조회한다.")
    @GetMapping("/{solveSessionId}/review")
    public ApiResponse<SolveSessionReviewResponse> getSolveSessionReview(
            @Parameter(description = "풀이 세션 ID", example = "9001")
            @PathVariable Long solveSessionId
    ) {

        // DB 연동 전 풀이 기록/해설 상세 확인용 더미 데이터
        SolveSessionReviewResponse response = new SolveSessionReviewResponse(
                solveSessionId,
                "완전 이진 트리 개념 문제집",
                SolveSessionType.CONCEPT_PRACTICE,
                2,
                1,
                1,
                180,
                LocalDateTime.now().minusDays(1),
                sampleReviewLogs()
        );

        return ApiResponse.success("완료된 문제집 기록/해설 상세 조회 성공", response);
    }

    // 풀이 유형별 화면 표시용 제목 매핑
    private String resolveTitle(SolveSessionType sessionType) {
        return switch (sessionType) {
            case CONCEPT_PRACTICE -> "완전 이진 트리 개념 문제집";
            case CATEGORY_LV1_MOCK -> "트리 챕터 모의고사";
            case CATEGORY_LV2_MOCK -> "이진 트리 챕터 모의고사";
            case SUBJECT_MOCK -> "자료구조 과목 모의고사";
            case DAILY_QUIZ -> "오늘의 CS 퀴즈";
        };
    }

    // 완료 기록/해설 화면용 문제별 풀이 로그 더미 데이터
    private List<SolveLogReviewResponse> sampleReviewLogs() {

        // 객관식 4지선다 선택지 더미 데이터
        List<QuestionChoiceResponse> choices = List.of(
                new QuestionChoiceResponse("A", "모든 노드가 정확히 두 개의 자식을 가진다."),
                new QuestionChoiceResponse("B", "마지막 레벨을 제외한 모든 레벨이 채워지고 마지막 레벨은 왼쪽부터 채워진다."),
                new QuestionChoiceResponse("C", "왼쪽 서브트리는 항상 오른쪽 서브트리보다 작다."),
                new QuestionChoiceResponse("D", "모든 리프 노드가 반드시 같은 깊이에 있어야 한다.")
        );

        // 문제별 선택 답, 정답, 해설, 풀이 시간 확인용 더미 데이터
        return List.of(
                new SolveLogReviewResponse(10001L, 5001L, 1, "완전 이진 트리에 대한 설명으로 가장 적절한 것은?", choices, "A", "B", false, "정답은 B입니다. 완전 이진 트리는 마지막 레벨이 왼쪽부터 채워지는 구조입니다.", 80, 45, true, 1, 1, true, false, LocalDateTime.now().minusDays(1)),
                new SolveLogReviewResponse(10002L, 5002L, 2, "포화 이진 트리와 완전 이진 트리의 차이로 적절한 것은?", choices, "B", "B", true, "포화 이진 트리는 모든 내부 노드가 두 자식을 갖는 구조이고, 완전 이진 트리는 레벨 채움 방식이 핵심입니다.", 40, 60, false, 0, 0, false, true, LocalDateTime.now().minusDays(1))
        );
    }
}