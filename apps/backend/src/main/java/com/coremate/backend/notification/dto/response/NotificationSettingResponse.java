package com.coremate.backend.notification.dto.response;

import com.coremate.backend.notification.entity.type.NotificationCategory;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "카테고리별 알림 수신 설정")
public record NotificationSettingResponse(
        @Schema(example = "FRIEND") NotificationCategory category,
        @Schema(example = "true") Boolean enabled
) {
}
