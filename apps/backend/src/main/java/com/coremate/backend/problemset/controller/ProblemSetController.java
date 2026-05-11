package com.coremate.backend.problemset.controller;

import com.coremate.backend.global.response.ApiResponse;
import com.coremate.backend.problemset.dto.response.ProblemSetDetailResponse;
import com.coremate.backend.problemset.dto.response.ProblemSetQuestionResponse;
import com.coremate.backend.problemset.dto.response.ProblemSetSummaryResponse;
import com.coremate.backend.problemset.dto.response.QuestionChoiceResponse;
import com.coremate.backend.problemset.dto.response.QuestionHintResponse;
import com.coremate.backend.problemset.dto.type.ProblemSetType;
import com.coremate.backend.problemset.dto.type.QuestionFormat;
import com.coremate.backend.problemset.dto.type.QuestionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "ProblemSet", description = "개념 문제집, 챕터 모의고사, 과목 모의고사 조회 API")
@RestController
@RequestMapping("/api/v1/problem-sets")
public class ProblemSetController {

    @Operation(summary = "문제집 요약 목록 조회", description = "문제집 페이지에서 완료/미완료 상태와 함께 문제집 목록을 보여준다.")
    @GetMapping("/summaries")
    public ApiResponse<List<ProblemSetSummaryResponse>> getProblemSetSummaries(
            @Parameter(description = "문제집 유형", example = "CONCEPT_PRACTICE") @RequestParam ProblemSetType problemSetType,
            @Parameter(description = "대상 상위 ID. 예: subjectId, categoryLv1Id, categoryLv2Id", example = "101") @RequestParam Long parentId,
            @Parameter(description = "임시 사용자 ID. 비로그인 체험이면 생략 가능", example = "1") @RequestParam(required = false) Long userId
    ) {
        return ApiResponse.success("문제집 요약 목록 조회 성공", List.of(
                new ProblemSetSummaryResponse(problemSetType, "CONCEPT", 1001L, "포화 이진 트리 개념 문제집", 5, false, 0, null, 0),
                new ProblemSetSummaryResponse(problemSetType, "CONCEPT", 1002L, "완전 이진 트리 개념 문제집", 5, true, 2, LocalDateTime.now().minusDays(1), 1)
        ));
    }

    @Operation(summary = "개념 문제집 상세 조회", description = "하위 개념 단위 문제집과 문제 목록을 조회한다. 개념 문제집은 문제별 즉시 피드백을 전제로 한다.")
    @GetMapping("/concepts/{conceptId}")
    public ApiResponse<ProblemSetDetailResponse> getConceptProblemSet(
            @Parameter(description = "개념 ID", example = "1002") @PathVariable Long conceptId,
            @Parameter(description = "임시 사용자 ID. 즐겨찾기 여부 표시용", example = "1") @RequestParam(required = false) Long userId
    ) {
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

    @Operation(summary = "대분류 챕터 모의고사 조회", description = "대분류 범위의 챕터 모의고사 문제 목록을 조회한다.")
    @GetMapping("/category-lv1/{categoryLv1Id}/mock")
    public ApiResponse<ProblemSetDetailResponse> getCategoryLv1Mock(
            @Parameter(description = "대분류 ID", example = "20") @PathVariable Long categoryLv1Id
    ) {
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

    @Operation(summary = "중분류 챕터 모의고사 조회", description = "중분류 범위의 챕터 모의고사 문제 목록을 조회한다.")
    @GetMapping("/category-lv2/{categoryLv2Id}/mock")
    public ApiResponse<ProblemSetDetailResponse> getCategoryLv2Mock(
            @Parameter(description = "중분류 ID", example = "101") @PathVariable Long categoryLv2Id
    ) {
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

    @Operation(summary = "과목 모의고사 조회", description = "과목 전체 범위의 모의고사 문제 목록을 조회한다.")
    @GetMapping("/subjects/{subjectId}/mock")
    public ApiResponse<ProblemSetDetailResponse> getSubjectMock(
            @Parameter(description = "과목 ID", example = "1") @PathVariable Long subjectId
    ) {
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

    @Operation(summary = "문제 힌트 조회", description = "문제 풀이 중 물음표 버튼을 눌렀을 때 common_mistake.correction_text 기반 힌트를 조회한다.")
    @GetMapping("/questions/{questionId}/hint")
    public ApiResponse<QuestionHintResponse> getQuestionHint(
            @Parameter(description = "문제 ID", example = "5001") @PathVariable Long questionId
    ) {
        QuestionHintResponse response = new QuestionHintResponse(
                questionId,
                3001L,
                "완전 이진 트리는 모든 레벨이 꽉 차야 하는 것이 아니라 마지막 레벨은 왼쪽부터 연속적으로 채워지면 됩니다."
        );
        return ApiResponse.success("문제 힌트 조회 성공", response);
    }

    private List<ProblemSetQuestionResponse> sampleQuestions() {
        List<QuestionChoiceResponse> choices = List.of(
                new QuestionChoiceResponse("A", "모든 노드가 정확히 두 개의 자식을 가진다."),
                new QuestionChoiceResponse("B", "마지막 레벨을 제외한 모든 레벨이 채워지고 마지막 레벨은 왼쪽부터 채워진다."),
                new QuestionChoiceResponse("C", "왼쪽 서브트리는 항상 오른쪽 서브트리보다 작다."),
                new QuestionChoiceResponse("D", "모든 리프 노드가 반드시 같은 깊이에 있어야 한다.")
        );

        return List.of(
                new ProblemSetQuestionResponse(5001L, 1002L, "완전 이진 트리", 1, QuestionFormat.MCQ_4, QuestionType.DEFINITION, "완전 이진 트리에 대한 설명으로 가장 적절한 것은?", choices, 2, 45, true, false),
                new ProblemSetQuestionResponse(5002L, 1002L, "완전 이진 트리", 2, QuestionFormat.MCQ_4, QuestionType.COMPARISON, "완전 이진 트리와 포화 이진 트리를 구분하는 설명으로 가장 적절한 것은?", choices, 3, 60, true, true)
        );
    }
}
