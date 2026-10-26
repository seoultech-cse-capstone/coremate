package com.coremate.backend.support.controller;

import com.coremate.backend.global.config.OpenApiConfig;
import com.coremate.backend.global.response.ApiResponse;
import com.coremate.backend.support.dto.request.AnswerInquiryRequest;
import com.coremate.backend.support.dto.response.BugReportResponse;
import com.coremate.backend.support.dto.response.InquiryResponse;
import com.coremate.backend.support.entity.type.BugReportStatus;
import com.coremate.backend.support.entity.type.InquiryStatus;
import com.coremate.backend.support.service.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AdminSupport", description = "관리자 문의/버그 신고 조회·답변 API")
@RestController
@RequestMapping("/api/v1/admin/support")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class AdminSupportController {

    private final SupportService supportService;

    @Operation(summary = "문의 목록 조회", description = "status 미지정 시 전체를 최신순으로 반환한다.")
    @GetMapping("/inquiries")
    public ApiResponse<List<InquiryResponse>> getInquiries(
            @RequestParam(required = false) InquiryStatus status
    ) {
        return ApiResponse.success(supportService.getInquiries(status));
    }

    @Operation(summary = "문의 상세 조회")
    @GetMapping("/inquiries/{inquiryId}")
    public ApiResponse<InquiryResponse> getInquiry(@PathVariable Long inquiryId) {
        return ApiResponse.success(supportService.getInquiry(inquiryId));
    }

    @Operation(summary = "문의 답변 등록", description = "답변을 저장하고 상태를 ANSWERED로 바꾼다.")
    @PatchMapping("/inquiries/{inquiryId}/answer")
    public ApiResponse<InquiryResponse> answerInquiry(
            @PathVariable Long inquiryId,
            @Valid @RequestBody AnswerInquiryRequest request
    ) {
        return ApiResponse.success("답변이 등록되었습니다.", supportService.answerInquiry(inquiryId, request));
    }

    @Operation(summary = "버그 신고 목록 조회", description = "status 미지정 시 전체를 최신순으로 반환한다.")
    @GetMapping("/bug-reports")
    public ApiResponse<List<BugReportResponse>> getBugReports(
            @RequestParam(required = false) BugReportStatus status
    ) {
        return ApiResponse.success(supportService.getBugReports(status));
    }

    @Operation(summary = "버그 신고 상태 변경")
    @PatchMapping("/bug-reports/{bugReportId}/status")
    public ApiResponse<BugReportResponse> updateBugReportStatus(
            @PathVariable Long bugReportId,
            @RequestParam BugReportStatus status
    ) {
        return ApiResponse.success("상태가 변경되었습니다.", supportService.updateBugReportStatus(bugReportId, status));
    }
}
