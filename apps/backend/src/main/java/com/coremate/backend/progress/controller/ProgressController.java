package com.coremate.backend.progress.controller;

import com.coremate.backend.global.config.OpenApiConfig;
import com.coremate.backend.global.response.ApiResponse;
import com.coremate.backend.problemset.dto.type.ProblemSetType;
import com.coremate.backend.progress.dto.response.ConceptProgressResponse;
import com.coremate.backend.progress.dto.response.ProblemSetProgressResponse;
import com.coremate.backend.progress.dto.response.SubjectProgressResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Progress", description = "문제집 완료 상태, 개념 완료 상태, 과목별 진행률 API")
@RestController
@RequestMapping("/api/v1/progress")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class ProgressController {

    // 문제집별 완료/미완료 상태 조회
    @Operation(summary = "문제집별 완료 상태 조회", description = "문제집 목록 오른쪽 완료/미완료 표시와 완료 횟수 표시를 위한 API")
    @GetMapping("/problem-sets")
    public ApiResponse<List<ProblemSetProgressResponse>> getProblemSetProgress(
            @Parameter(description = "임시 사용자 ID", example = "1")
            @RequestParam(defaultValue = "1") Long userId,

            @Parameter(description = "문제집 유형", example = "CONCEPT_PRACTICE")
            @RequestParam(required = false) ProblemSetType problemSetType,

            @Parameter(description = "과목 ID", example = "1")
            @RequestParam(required = false) Long subjectId,

            @Parameter(description = "대분류 ID", example = "20")
            @RequestParam(required = false) Long categoryLv1Id,

            @Parameter(description = "중분류 ID", example = "101")
            @RequestParam(required = false) Long categoryLv2Id
    ) {

        // DB 연동 전 문제집 진행 상태 확인용 더미 데이터
        return ApiResponse.success("문제집별 완료 상태 조회 성공", List.of(
                new ProblemSetProgressResponse(7001L, userId, ProblemSetType.CONCEPT_PRACTICE, "CONCEPT", 1001L, 1L, 20L, 101L, 1001L, 9001L, true, 2, LocalDateTime.now().minusDays(1)),
                new ProblemSetProgressResponse(7002L, userId, ProblemSetType.CATEGORY_LV2_MOCK, "CATEGORY_LV2", 101L, 1L, 20L, 101L, null, 9002L, true, 1, LocalDateTime.now().minusDays(2))
        ));
    }

    // 하위 개념별 완료 체크 상태 조회
    @Operation(summary = "하위 개념 완료 상태 조회", description = "하위 개념 리스트에서 회원 사용자에게 완료 체크를 보여주기 위한 API")
    @GetMapping("/concepts")
    public ApiResponse<List<ConceptProgressResponse>> getConceptProgress(
            @Parameter(description = "임시 사용자 ID", example = "1")
            @RequestParam(defaultValue = "1") Long userId,

            @Parameter(description = "중분류 ID", example = "101")
            @RequestParam Long categoryLv2Id
    ) {

        // DB 연동 전 개념 완료 상태 확인용 더미 데이터
        return ApiResponse.success("하위 개념 완료 상태 조회 성공", List.of(
                new ConceptProgressResponse(8001L, userId, 1001L, "포화 이진 트리", categoryLv2Id, false, null, null),
                new ConceptProgressResponse(8002L, userId, 1002L, "완전 이진 트리", categoryLv2Id, true, 9001L, LocalDateTime.now().minusDays(1))
        ));
    }

    // 과목별 학습 진행률 및 스탯 조회
    @Operation(summary = "과목별 학습 진행률 조회", description = "마이페이지의 과목별 학습 달성률과 육각형 스탯에 사용할 데이터를 조회한다.")
    @GetMapping("/subjects")
    public ApiResponse<List<SubjectProgressResponse>> getSubjectProgress(
            @Parameter(description = "임시 사용자 ID", example = "1")
            @RequestParam(defaultValue = "1") Long userId
    ) {

        // DB 연동 전 과목별 진행률 확인용 더미 데이터
        return ApiResponse.success("과목별 학습 진행률 조회 성공", List.of(
                new SubjectProgressResponse(userId, 1L, "자료구조", 120, 96, 24, 12, 40, 3, 1, 72, 30.0),
                new SubjectProgressResponse(userId, 2L, "알고리즘", 80, 60, 20, 8, 35, 2, 0, 58, 22.8)
        ));
    }
}