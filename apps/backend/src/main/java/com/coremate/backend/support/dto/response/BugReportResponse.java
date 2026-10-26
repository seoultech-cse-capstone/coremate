package com.coremate.backend.support.dto.response;

import com.coremate.backend.support.entity.BugReport;
import com.coremate.backend.support.entity.type.BugReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "버그 신고 항목")
public record BugReportResponse(
        @Schema(example = "1") Long bugReportId,
        @Schema(example = "42") Long userId,
        @Schema(example = "데모용주") String nickname,
        @Schema(example = "문제를 풀고 제출하면 앱이 종료됩니다.") String body,
        @Schema(example = "RECEIVED") BugReportStatus status,
        @Schema(example = "2026-10-26T19:10:11") LocalDateTime createdAt
) {

    public static BugReportResponse from(BugReport bugReport) {
        return new BugReportResponse(
                bugReport.getBugReportId(),
                bugReport.getUser().getUserId(),
                bugReport.getUser().getNickname(),
                bugReport.getBody(),
                bugReport.getStatus(),
                bugReport.getCreatedAt()
        );
    }
}
