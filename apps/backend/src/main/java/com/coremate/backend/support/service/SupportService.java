package com.coremate.backend.support.service;

import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.support.dto.request.AnswerInquiryRequest;
import com.coremate.backend.support.dto.request.CreateBugReportRequest;
import com.coremate.backend.support.dto.request.CreateInquiryRequest;
import com.coremate.backend.support.dto.response.BugReportResponse;
import com.coremate.backend.support.dto.response.InquiryResponse;
import com.coremate.backend.support.entity.BugReport;
import com.coremate.backend.support.entity.Inquiry;
import com.coremate.backend.support.entity.type.BugReportStatus;
import com.coremate.backend.support.entity.type.InquiryStatus;
import com.coremate.backend.support.repository.BugReportRepository;
import com.coremate.backend.support.repository.InquiryRepository;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupportService {

    private final InquiryRepository inquiryRepository;
    private final BugReportRepository bugReportRepository;
    private final UserRepository userRepository;

    // --- 사용자 ---

    @Transactional
    public InquiryResponse createInquiry(Long userId, CreateInquiryRequest request) {
        User user = findUser(userId);
        Inquiry inquiry = inquiryRepository.save(
                new Inquiry(user, request.getCategory(), request.getTitle(), request.getBody()));
        return InquiryResponse.from(inquiry);
    }

    @Transactional
    public BugReportResponse createBugReport(Long userId, CreateBugReportRequest request) {
        User user = findUser(userId);
        BugReport bugReport = bugReportRepository.save(new BugReport(user, request.getBody()));
        return BugReportResponse.from(bugReport);
    }

    public List<InquiryResponse> getMyInquiries(Long userId) {
        return inquiryRepository.findByUserUserIdOrderByCreatedAtDesc(userId).stream()
                .map(InquiryResponse::from)
                .toList();
    }

    // 타인의 문의 존재 여부를 노출하지 않도록, 소유자가 아니면 NOT_FOUND로 응답한다.
    public InquiryResponse getMyInquiry(Long userId, Long inquiryId) {
        return inquiryRepository.findByInquiryIdAndUserUserId(inquiryId, userId)
                .map(InquiryResponse::from)
                .orElseThrow(() -> new CoreMateException(ErrorCode.INQUIRY_NOT_FOUND));
    }

    // --- 관리자 ---

    public List<InquiryResponse> getInquiries(InquiryStatus status) {
        List<Inquiry> inquiries = status == null
                ? inquiryRepository.findAllByOrderByCreatedAtDesc()
                : inquiryRepository.findByStatusOrderByCreatedAtDesc(status);
        return inquiries.stream().map(InquiryResponse::from).toList();
    }

    public InquiryResponse getInquiry(Long inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .map(InquiryResponse::from)
                .orElseThrow(() -> new CoreMateException(ErrorCode.INQUIRY_NOT_FOUND));
    }

    @Transactional
    public InquiryResponse answerInquiry(Long inquiryId, AnswerInquiryRequest request) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.INQUIRY_NOT_FOUND));
        inquiry.answer(request.getAnswer());
        return InquiryResponse.from(inquiry);
    }

    public List<BugReportResponse> getBugReports(BugReportStatus status) {
        List<BugReport> bugReports = status == null
                ? bugReportRepository.findAllByOrderByCreatedAtDesc()
                : bugReportRepository.findByStatusOrderByCreatedAtDesc(status);
        return bugReports.stream().map(BugReportResponse::from).toList();
    }

    @Transactional
    public BugReportResponse updateBugReportStatus(Long bugReportId, BugReportStatus status) {
        BugReport bugReport = bugReportRepository.findById(bugReportId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.BUG_REPORT_NOT_FOUND));
        bugReport.updateStatus(status);
        return BugReportResponse.from(bugReport);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.USER_NOT_FOUND));
    }
}
