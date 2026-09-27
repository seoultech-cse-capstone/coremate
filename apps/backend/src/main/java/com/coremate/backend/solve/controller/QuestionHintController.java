package com.coremate.backend.solve.controller;

import com.coremate.backend.global.response.ApiResponse;
import com.coremate.backend.solve.dto.response.QuestionHintResponse;
import com.coremate.backend.solve.service.QuestionHintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Solve", description = "문제 풀이 세션 생성, 답안 제출, 완료 처리 API")
@RestController
@RequestMapping("/api/v1/solve")
@RequiredArgsConstructor
public class QuestionHintController {

    private final QuestionHintService questionHintService;

    @Operation(
            summary = "문제 힌트 조회 (비회원/회원 공통)",
            description = "문제 풀이 중 물음표 버튼을 눌렀을 때 common_mistake.correction_text 기반 힌트를 조회한다. "
                    + "비회원/회원 모두 사용 가능하며, 정답을 직접 노출하지 않는 방향성 힌트만 반환한다. "
                    + "힌트가 연결되어 있지 않거나 비활성 상태인 경우 404 를 반환한다."
    )
    @GetMapping("/questions/{questionId}/hint")
    public ApiResponse<QuestionHintResponse> getQuestionHint(
            @Parameter(description = "문제 ID", example = "5001")
            @PathVariable Long questionId
    ) {
        return ApiResponse.success("문제 힌트 조회 성공",
                questionHintService.getQuestionHint(questionId));
    }
}
