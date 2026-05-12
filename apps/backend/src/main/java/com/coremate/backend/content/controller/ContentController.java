package com.coremate.backend.content.controller;

import com.coremate.backend.content.dto.response.CategoryLv1Response;
import com.coremate.backend.content.dto.response.CategoryLv2Response;
import com.coremate.backend.content.dto.response.ConceptDetailResponse;
import com.coremate.backend.content.dto.response.ConceptResponse;
import com.coremate.backend.content.dto.response.SourceRefResponse;
import com.coremate.backend.content.dto.response.SubjectResponse;
import com.coremate.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Content", description = "과목, 대분류, 중분류, 하위 개념 탐색 API")
@RestController
@RequestMapping("/api/v1/content")
public class ContentController {

    // CS 과목 목록 조회
    @Operation(summary = "과목 목록 조회", description = "CoreMate에서 제공하는 6개 CS 과목 목록을 조회한다.")
    @GetMapping("/subjects")
    public ApiResponse<List<SubjectResponse>> getSubjects() {

        // DB 연동 전 응답 구조 확인용 더미 데이터
        return ApiResponse.success("과목 목록 조회 성공", List.of(
                new SubjectResponse(1L, "DS", "자료구조", "Data Structures", 1, true),
                new SubjectResponse(2L, "ALGO", "알고리즘", "Algorithms", 2, true),
                new SubjectResponse(3L, "OS", "운영체제", "Operating Systems", 3, true),
                new SubjectResponse(4L, "DB", "데이터베이스", "Database", 4, true),
                new SubjectResponse(5L, "CA", "컴퓨터구조", "Computer Architecture", 5, true),
                new SubjectResponse(6L, "NETWORK", "네트워크", "Computer Network", 6, true)
        ));
    }

    // 선택 과목 하위 대분류 목록 조회
    @Operation(summary = "대분류 목록 조회", description = "선택한 과목 아래의 대분류 목록을 조회한다.")
    @GetMapping("/subjects/{subjectId}/category-lv1")
    public ApiResponse<List<CategoryLv1Response>> getCategoryLv1List(
            @Parameter(description = "과목 ID", example = "1") @PathVariable Long subjectId
    ) {

        // DB 연동 전 응답 구조 확인용 더미 데이터
        return ApiResponse.success("대분류 목록 조회 성공", List.of(
                new CategoryLv1Response(10L, subjectId, "DS_LINEAR", "선형 자료구조", "Linear Data Structures", "자료가 순차적으로 연결되는 구조", 1, true),
                new CategoryLv1Response(20L, subjectId, "DS_TREE", "트리", "Tree", "계층 구조를 표현하는 비선형 자료구조", 2, true),
                new CategoryLv1Response(30L, subjectId, "DS_GRAPH", "그래프", "Graph", "정점과 간선으로 관계를 표현하는 자료구조", 3, true)
        ));
    }

    // 선택 대분류 하위 중분류 목록 조회
    @Operation(summary = "중분류 목록 조회", description = "선택한 대분류 아래의 중분류 목록을 조회한다.")
    @GetMapping("/category-lv1/{categoryLv1Id}/category-lv2")
    public ApiResponse<List<CategoryLv2Response>> getCategoryLv2List(
            @Parameter(description = "대분류 ID", example = "20") @PathVariable Long categoryLv1Id
    ) {

        // DB 연동 전 응답 구조 확인용 더미 데이터
        return ApiResponse.success("중분류 목록 조회 성공", List.of(
                new CategoryLv2Response(101L, categoryLv1Id, "DS_TREE_BINARY_TREE", "이진 트리", "Binary Tree", "각 노드가 최대 두 개의 자식을 갖는 트리", 1, true),
                new CategoryLv2Response(102L, categoryLv1Id, "DS_TREE_HEAP", "힙", "Heap", "완전 이진 트리를 기반으로 한 우선순위 자료구조", 2, true)
        ));
    }

    // 선택 중분류 하위 개념 목록 조회
    @Operation(summary = "하위 개념 목록 조회", description = "선택한 중분류 아래의 실제 학습 단위 개념 목록을 조회한다.")
    @GetMapping("/category-lv2/{categoryLv2Id}/concepts")
    public ApiResponse<List<ConceptResponse>> getConcepts(
            @Parameter(description = "중분류 ID", example = "101") @PathVariable Long categoryLv2Id,

            // 회원 기준 완료 여부 표시용 임시 userId
            @Parameter(description = "회원 기준 완료 여부 표시용 임시 사용자 ID. 비로그인 체험이면 생략 가능", example = "1")
            @RequestParam(required = false) Long userId
    ) {

        // completed 값 포함 개념 목록 더미 데이터
        return ApiResponse.success("하위 개념 목록 조회 성공", List.of(
                new ConceptResponse(1001L, "DS_TREE_FULL_BINARY_TREE", categoryLv2Id, "포화 이진 트리", "Full Binary Tree", "모든 노드가 0개 또는 2개의 자식을 갖는 이진 트리", false, 1),
                new ConceptResponse(1002L, "DS_TREE_COMPLETE_BINARY_TREE", categoryLv2Id, "완전 이진 트리", "Complete Binary Tree", "마지막 레벨을 제외한 모든 레벨이 채워지고 마지막 레벨은 왼쪽부터 채워지는 이진 트리", true, 2)
        ));
    }

    // 하위 개념 상세 정보 조회
    @Operation(summary = "하위 개념 상세 조회", description = "개념 문제집 시작 전 또는 개념 설명 화면에서 사용할 하위 개념 상세 정보를 조회한다.")
    @GetMapping("/concepts/{conceptId}")
    public ApiResponse<ConceptDetailResponse> getConceptDetail(
            @Parameter(description = "개념 ID", example = "1002") @PathVariable Long conceptId
    ) {

        // 개념 상세 화면용 더미 데이터
        ConceptDetailResponse response = new ConceptDetailResponse(
                conceptId,
                "DS_TREE_COMPLETE_BINARY_TREE",
                1L,
                "자료구조",
                20L,
                "트리",
                101L,
                "이진 트리",
                "완전 이진 트리",
                "Complete Binary Tree",
                List.of("완전이진트리", "Complete Binary Tree", "complete binary tree"),
                "마지막 레벨을 제외한 모든 레벨이 채워지고 마지막 레벨은 왼쪽부터 채워지는 이진 트리",
                "완전 이진 트리는 배열 기반 힙 구현에서 자주 사용되는 이진 트리 형태다. 노드는 위 레벨부터 채워지며 마지막 레벨에서는 왼쪽부터 연속적으로 배치된다.",
                List.of("이진 트리", "마지막 레벨", "왼쪽 채움", "힙"),
                List.of("마지막 레벨을 제외한 레벨은 모두 채워진다.", "마지막 레벨의 노드는 왼쪽부터 연속적으로 채워진다.", "배열로 표현하기 쉽다."),
                List.of(
                        new SourceRefResponse("DATA_STRUCTURE_open_data_structures_p120", "open_data_structures.pdf", 120),
                        new SourceRefResponse("DATA_STRUCTURE_open_data_structures_p121", "open_data_structures.pdf", 121)
                ),
                "approved",
                true
        );

        return ApiResponse.success("하위 개념 상세 조회 성공", response);
    }
}