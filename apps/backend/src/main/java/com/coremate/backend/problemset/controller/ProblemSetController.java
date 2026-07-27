package com.coremate.backend.problemset.controller;

import com.coremate.backend.global.config.OpenApiConfig;
import com.coremate.backend.global.response.ApiResponse;
import com.coremate.backend.global.security.CustomUserDetails;
import com.coremate.backend.problemset.dto.response.CompletedProblemSetDetailResponse;
import com.coremate.backend.problemset.dto.response.CompletedProblemSetSummaryResponse;
import com.coremate.backend.problemset.dto.response.ProblemSetDetailResponse;
import com.coremate.backend.problemset.dto.response.ProblemSetQuestionResponse;
import com.coremate.backend.problemset.dto.response.ProblemSetSummaryResponse;
import com.coremate.backend.problemset.dto.response.QuestionChoiceResponse;
import com.coremate.backend.problemset.dto.type.ProblemSetType;
import com.coremate.backend.problemset.dto.type.QuestionFormat;
import com.coremate.backend.problemset.dto.type.QuestionType;
import com.coremate.backend.problemset.service.CompletedProblemSetService;
import com.coremate.backend.problemset.service.ProblemSetService;
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

@Tag(name = "ProblemSet", description = "개념 문제집, 챕터 모의고사, 과목 모의고사 조회 API")
@RestController
@RequestMapping("/api/v1/problem-sets")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@RequiredArgsConstructor
public class ProblemSetController {

    private final CompletedProblemSetService completedProblemSetService;
    private final ProblemSetService problemSetService;

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

    // 문제집 목록 요약 정보 조회
    @Operation(summary = "문제집 요약 목록 조회",
            description = "중분류(parentId) 하위의 개념 문제집 목록을 풀이 상태(미풀이/진행중/완료)와 함께 반환한다. "
                    + "로그인 시 인증 토큰의 본인 풀이 기록 기준으로 상태를 계산하고, 비로그인은 전부 미풀이로 반환한다. "
                    + "완료 기록이 있으면 최고 점수 회차 기준 총 문제 수/정답 수/합격 여부(전 문제 정답 시 PASS)를 포함한다.")
    @GetMapping("/summaries")
    public ApiResponse<List<ProblemSetSummaryResponse>> getProblemSetSummaries(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "문제집 유형 (현재 CONCEPT_PRACTICE 지원)", example = "CONCEPT_PRACTICE")
            @RequestParam ProblemSetType problemSetType,

            @Parameter(description = "대상 상위 ID. 개념 문제집이면 중분류(categoryLv2) ID", example = "101")
            @RequestParam Long parentId
    ) {
        Long userId = (userDetails != null) ? userDetails.getUserId() : null;
        return ApiResponse.success("문제집 요약 목록 조회 성공",
                problemSetService.getProblemSetSummaries(problemSetType, parentId, userId));
    }

    // 하위 개념 기준 문제집 상세 조회
    @Operation(summary = "개념 문제집 상세 조회", description = "하위 개념 단위 문제집과 문제 목록을 조회한다. 개념 문제집은 문제별 즉시 피드백을 전제로 한다.")
    @GetMapping("/concepts/{conceptId}")
    public ApiResponse<ProblemSetDetailResponse> getConceptProblemSet(
            @Parameter(description = "개념 ID", example = "1002")
            @PathVariable Long conceptId,

            @Parameter(description = "임시 사용자 ID. 즐겨찾기 여부 표시용", example = "1")
            @RequestParam(required = false) Long userId
    ) {

        // 개념 문제집 상세 응답 구조 확인용 더미 데이터
        return ApiResponse.success("개념 문제집 상세 조회 성공", new ProblemSetDetailResponse(
                ProblemSetType.CONCEPT_PRACTICE,
                "CONCEPT",
                conceptId,
                "완전 이진 트리 개념 문제집",
                "완전 이진 트리의 정의와 포화 이진 트리와의 차이를 확인하는 문제집",
                sampleQuestions().size(),
                null,
                true,
                sampleQuestions()
        ));
    }

    // 대분류 범위 챕터 모의고사 조회
    @Operation(summary = "대분류 챕터 모의고사 조회", description = "대분류 범위의 챕터 모의고사 문제 목록을 조회한다.")
    @GetMapping("/category-lv1/{categoryLv1Id}/mock")
    public ApiResponse<ProblemSetDetailResponse> getCategoryLv1Mock(
            @Parameter(description = "대분류 ID", example = "20")
            @PathVariable Long categoryLv1Id
    ) {

        // 대분류 모의고사 응답 구조 확인용 더미 데이터
        return ApiResponse.success("대분류 챕터 모의고사 조회 성공", new ProblemSetDetailResponse(
                ProblemSetType.CATEGORY_LV1_MOCK,
                "CATEGORY_LV1",
                categoryLv1Id,
                "트리 챕터 모의고사",
                "트리 대분류 전체 개념을 점검하는 모의고사",
                sampleQuestions().size(),
                900,
                false,
                sampleQuestions()
        ));
    }

    // 중분류 범위 챕터 모의고사 조회
    @Operation(summary = "중분류 챕터 모의고사 조회", description = "중분류 범위의 챕터 모의고사 문제 목록을 조회한다.")
    @GetMapping("/category-lv2/{categoryLv2Id}/mock")
    public ApiResponse<ProblemSetDetailResponse> getCategoryLv2Mock(
            @Parameter(description = "중분류 ID", example = "101")
            @PathVariable Long categoryLv2Id
    ) {

        // 중분류 모의고사 응답 구조 확인용 더미 데이터
        return ApiResponse.success("중분류 챕터 모의고사 조회 성공", new ProblemSetDetailResponse(
                ProblemSetType.CATEGORY_LV2_MOCK,
                "CATEGORY_LV2",
                categoryLv2Id,
                "이진 트리 챕터 모의고사",
                "이진 트리 관련 하위 개념을 점검하는 모의고사",
                sampleQuestions().size(),
                600,
                false,
                sampleQuestions()
        ));
    }

    // 과목 전체 범위 모의고사 조회
    @Operation(summary = "과목 모의고사 조회", description = "과목 전체 범위의 모의고사 문제 목록을 조회한다.")
    @GetMapping("/subjects/{subjectId}/mock")
    public ApiResponse<ProblemSetDetailResponse> getSubjectMock(
            @Parameter(description = "과목 ID", example = "1")
            @PathVariable Long subjectId
    ) {

        // 과목 모의고사 응답 구조 확인용 더미 데이터
        return ApiResponse.success("과목 모의고사 조회 성공", new ProblemSetDetailResponse(
                ProblemSetType.SUBJECT_MOCK,
                "SUBJECT",
                subjectId,
                "자료구조 과목 모의고사",
                "자료구조 과목 전체 범위를 점검하는 모의고사",
                sampleQuestions().size(),
                1200,
                false,
                sampleQuestions()
        ));
    }


    // 문제집/모의고사 공통 문제 목록 더미 데이터
    private List<ProblemSetQuestionResponse> sampleQuestions() {

        // 객관식 4지선다 선택지 더미 데이터
        List<QuestionChoiceResponse> choices = List.of(
                new QuestionChoiceResponse("A", "모든 노드가 정확히 두 개의 자식을 가진다."),
                new QuestionChoiceResponse("B", "마지막 레벨을 제외한 모든 레벨이 채워지고 마지막 레벨은 왼쪽부터 채워진다."),
                new QuestionChoiceResponse("C", "왼쪽 서브트리는 항상 오른쪽 서브트리보다 작다."),
                new QuestionChoiceResponse("D", "모든 리프 노드가 반드시 같은 깊이에 있어야 한다.")
        );

        // 문제 응답 구조 확인용 더미 데이터
        return List.of(
                new ProblemSetQuestionResponse(5001L, 1002L, "완전 이진 트리", 1, QuestionFormat.MCQ_4, QuestionType.DEFINITION, "완전 이진 트리에 대한 설명으로 가장 적절한 것은?", choices, 2, 45, true, false),
                new ProblemSetQuestionResponse(5002L, 1002L, "완전 이진 트리", 2, QuestionFormat.MCQ_4, QuestionType.COMPARISON, "완전 이진 트리와 포화 이진 트리를 구분하는 설명으로 가장 적절한 것은?", choices, 3, 60, true, true)
        );
    }
}