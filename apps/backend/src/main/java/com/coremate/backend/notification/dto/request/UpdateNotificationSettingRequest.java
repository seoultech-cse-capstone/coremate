package com.coremate.backend.notification.dto.request;

import com.coremate.backend.notification.entity.type.NotificationCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "알림 수신 설정 변경 요청")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNotificationSettingRequest {

    @Schema(description = "설정할 카테고리. SYSTEM 은 변경할 수 없다", example = "FRIEND")
    @NotNull
    private NotificationCategory category;

    @Schema(example = "false")
    @NotNull
    private Boolean enabled;
}
