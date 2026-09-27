package com.coremate.backend.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "읽지 않은 알림 개수")
public record UnreadNotificationCountResponse(
        @Schema(example = "3") Long unreadCount
) {
}
