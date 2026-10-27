package com.coremate.backend.support.dto.response;

import com.coremate.backend.support.entity.Inquiry;
import com.coremate.backend.support.entity.type.InquiryCategory;
import com.coremate.backend.support.entity.type.InquiryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "문의 항목")
public record InquiryResponse(
        @Schema(example = "1") Long inquiryId,
        @Schema(example = "42") Long userId,
        @Schema(example = "데모용주") String nickname,
        @Schema(example = "PROBLEM_SET") InquiryCategory category,
        @Schema(example = "문제집 화면 관련 문의") String title,
        @Schema(example = "특정 문제집을 열면 로딩이 멈춥니다.") String body,
        @Schema(example = "PENDING") InquiryStatus status,
        @Schema(example = "확인 결과 수정되었습니다.") String answer,
        @Schema(example = "2026-10-26T19:12:00") LocalDateTime answeredAt,
        @Schema(example = "2026-10-26T19:10:11") LocalDateTime createdAt
) {

    public static InquiryResponse from(Inquiry inquiry) {
        return new InquiryResponse(
                inquiry.getInquiryId(),
                inquiry.getUser().getUserId(),
                inquiry.getUser().getNickname(),
                inquiry.getCategory(),
                inquiry.getTitle(),
                inquiry.getBody(),
                inquiry.getStatus(),
                inquiry.getAnswer(),
                inquiry.getAnsweredAt(),
                inquiry.getCreatedAt()
        );
    }
}
