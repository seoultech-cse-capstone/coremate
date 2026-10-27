package com.coremate.backend.support.controller;

import com.coremate.backend.global.config.OpenApiConfig;
import com.coremate.backend.global.response.ApiResponse;
import com.coremate.backend.global.security.CustomUserDetails;
import com.coremate.backend.support.dto.request.CreateBugReportRequest;
import com.coremate.backend.support.dto.request.CreateInquiryRequest;
import com.coremate.backend.support.dto.response.BugReportResponse;
import com.coremate.backend.support.dto.response.InquiryResponse;
import com.coremate.backend.support.service.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Support", description = "문의하기, 버그 신고 API")
@RestController
@RequestMapping("/api/v1/support")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class SupportController {

    private final SupportService supportService;

    @Operation(summary = "문의 등록", description = "제목, 카테고리, 본문(300자 이내)으로 문의를 등록한다.")
    @PostMapping("/inquiries")
    public ApiResponse<InquiryResponse> createInquiry(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateInquiryRequest request
    ) {
        InquiryResponse response = supportService.createInquiry(userDetails.getUserId(), request);
        return ApiResponse.success("문의가 접수되었습니다.", response);
    }

    @Operation(summary = "버그 신고", description = "앱 사용 중 발생한 오류 증상을 본문으로 신고한다.")
    @PostMapping("/bug-reports")
    public ApiResponse<BugReportResponse> createBugReport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateBugReportRequest request
    ) {
        BugReportResponse response = supportService.createBugReport(userDetails.getUserId(), request);
        return ApiResponse.success("버그 신고가 접수되었습니다.", response);
    }

    @Operation(summary = "내 문의 목록 조회", description = "로그인한 사용자가 등록한 문의를 최신순으로 반환한다.")
    @GetMapping("/inquiries")
    public ApiResponse<List<InquiryResponse>> getMyInquiries(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.success(supportService.getMyInquiries(userDetails.getUserId()));
    }

    @Operation(summary = "내 문의 상세 조회", description = "본인이 등록한 문의만 조회할 수 있다.")
    @GetMapping("/inquiries/{inquiryId}")
    public ApiResponse<InquiryResponse> getMyInquiry(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long inquiryId
    ) {
        return ApiResponse.success(supportService.getMyInquiry(userDetails.getUserId(), inquiryId));
    }
}
