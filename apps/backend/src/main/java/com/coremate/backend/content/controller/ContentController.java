package com.coremate.backend.content.controller;

import com.coremate.backend.content.dto.response.CategoryLv1Response;
import com.coremate.backend.content.dto.response.CategoryLv2Response;
import com.coremate.backend.content.dto.response.ConceptDetailResponse;
import com.coremate.backend.content.dto.response.ConceptResponse;
import com.coremate.backend.content.dto.response.SubjectResponse;
import com.coremate.backend.content.service.ContentService;
import com.coremate.backend.global.response.ApiResponse;
import com.coremate.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Content", description = "과목, 대분류, 중분류, 하위 개념 탐색 API")
@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @Operation(summary = "과목 목록 조회", description = "CoreMate에서 제공하는 CS 과목 목록을 조회한다.")
    @GetMapping("/subjects")
    public ApiResponse<List<SubjectResponse>> getSubjects() {
        return ApiResponse.success("과목 목록 조회 성공", contentService.getSubjects());
    }

    @Operation(summary = "대분류 목록 조회", description = "선택한 과목 아래의 대분류 목록을 조회한다.")
    @GetMapping("/subjects/{subjectId}/category-lv1")
    public ApiResponse<List<CategoryLv1Response>> getCategoryLv1List(
            @Parameter(description = "과목 ID", example = "1") @PathVariable Long subjectId
    ) {
        return ApiResponse.success("대분류 목록 조회 성공", contentService.getCategoryLv1List(subjectId));
    }

    @Operation(summary = "중분류 목록 조회", description = "선택한 대분류 아래의 중분류 목록을 조회한다.")
    @GetMapping("/category-lv1/{categoryLv1Id}/category-lv2")
    public ApiResponse<List<CategoryLv2Response>> getCategoryLv2List(
            @Parameter(description = "대분류 ID", example = "20") @PathVariable Long categoryLv1Id
    ) {
        return ApiResponse.success("중분류 목록 조회 성공", contentService.getCategoryLv2List(categoryLv1Id));
    }

    @Operation(summary = "하위 개념 목록 조회",
            description = "선택한 중분류 아래의 개념 목록을 풀이 상태(미풀이/진행중/완료)와 함께 조회한다. "
                    + "로그인 시 인증 토큰의 본인 풀이 기록 기준으로 상태를 계산하고, 비로그인은 전부 미풀이로 반환한다.")
    @GetMapping("/category-lv2/{categoryLv2Id}/concepts")
    public ApiResponse<List<ConceptResponse>> getConcepts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "중분류 ID", example = "101") @PathVariable Long categoryLv2Id
    ) {
        Long userId = (userDetails != null) ? userDetails.getUserId() : null;
        return ApiResponse.success("하위 개념 목록 조회 성공", contentService.getConcepts(categoryLv2Id, userId));
    }

    @Operation(summary = "하위 개념 상세 조회", description = "개념 문제집 시작 전 또는 개념 설명 화면에서 사용할 하위 개념 상세 정보를 조회한다.")
    @GetMapping("/concepts/{conceptId}")
    public ApiResponse<ConceptDetailResponse> getConceptDetail(
            @Parameter(description = "개념 ID", example = "1002") @PathVariable Long conceptId
    ) {
        return ApiResponse.success("하위 개념 상세 조회 성공", contentService.getConceptDetail(conceptId));
    }
}
