package com.coremate.backend.auth.dto.response;

import com.coremate.backend.auth.entity.UserSession;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "로그인된 기기 한 대")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionItem {

    @Schema(description = "해지 요청에 사용하는 세션 식별자",
            example = "5a1c9d02-8e3f-4b77-91aa-0c2d6f8e4b13")
    private String sessionId;

    @Schema(example = "iPhone 15 Pro")
    private String deviceName;

    @Schema(example = "IOS")
    private String platform;

    @Schema(example = "18.2")
    private String osVersion;

    @Schema(example = "2026-08-04T17:41:00")
    private LocalDateTime lastSeenAt;

    @Schema(description = "이 기기가 처음 등록된 시각. IP·위치를 수집하지 않으므로 "
            + "사용자가 본인 기기인지 판별할 수 있는 위조 불가한 근거는 이 값뿐이다.",
            example = "2026-07-20T09:12:00")
    private LocalDateTime createdAt;

    @Schema(description = "지금 이 요청을 보낸 기기인지. 서버가 토큰의 sid 와 비교해 계산한다.",
            example = "true")
    private boolean current;

    public static SessionItem of(UserSession session, String currentSessionId) {
        return SessionItem.builder()
                .sessionId(session.getSessionId())
                .deviceName(session.getDeviceName())
                .platform(session.getPlatform().name())
                .osVersion(session.getOsVersion())
                .lastSeenAt(session.getLastSeenAt())
                .createdAt(session.getCreatedAt())
                .current(session.getSessionId().equals(currentSessionId))
                .build();
    }
}
