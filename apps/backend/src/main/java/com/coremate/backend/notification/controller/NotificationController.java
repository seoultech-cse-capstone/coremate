package com.coremate.backend.notification.controller;

import com.coremate.backend.global.security.CustomUserDetails;
import com.coremate.backend.notification.dto.request.RegisterDeviceTokenRequest;
import com.coremate.backend.notification.dto.request.UpdateNotificationSettingRequest;
import com.coremate.backend.notification.dto.response.NotificationResponse;
import com.coremate.backend.notification.dto.response.NotificationSettingResponse;
import com.coremate.backend.notification.dto.response.UnreadNotificationCountResponse;
import com.coremate.backend.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification", description = "기기 토큰, 알림 수신 설정, 인앱 알림함 API")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "기기 토큰 등록",
            description = "로그인 직후와 토큰 갱신 시 호출한다. 같은 사용자·기기 조합이면 기존 행의 토큰을 갱신한다."
    )
    @PostMapping("/device-tokens")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerDeviceToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody RegisterDeviceTokenRequest request
    ) {
        notificationService.registerDeviceToken(userDetails.getUserId(), request);
    }

    @Operation(
            summary = "기기 토큰 해제",
            description = "로그아웃 시 호출한다. 행을 지우지 않고 비활성으로 두어 이후 발송 대상에서 제외한다."
    )
    @DeleteMapping("/device-tokens")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregisterDeviceToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String token
    ) {
        notificationService.unregisterDeviceToken(userDetails.getUserId(), token);
    }

    @Operation(
            summary = "알림 수신 설정 조회",
            description = "설정한 적 없는 카테고리는 수신 허용으로 응답한다. 계정 보안 알림(SYSTEM)은 목록에 포함되지 않는다."
    )
    @GetMapping("/settings")
    public List<NotificationSettingResponse> getSettings(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return notificationService.getSettings(userDetails.getUserId());
    }

    @Operation(summary = "알림 수신 설정 변경")
    @PutMapping("/settings")
    public NotificationSettingResponse updateSetting(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateNotificationSettingRequest request
    ) {
        return notificationService.updateSetting(userDetails.getUserId(), request);
    }

    @Operation(summary = "알림함 조회", description = "본인 알림을 최신순으로 최대 100건 반환한다.")
    @GetMapping
    public List<NotificationResponse> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return notificationService.getNotifications(userDetails.getUserId());
    }

    @Operation(summary = "읽지 않은 알림 개수 조회", description = "홈 화면의 알림 배지에 쓴다.")
    @GetMapping("/unread-count")
    public UnreadNotificationCountResponse getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return new UnreadNotificationCountResponse(notificationService.getUnreadCount(userDetails.getUserId()));
    }

    @Operation(summary = "알림 읽음 처리")
    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationId
    ) {
        return notificationService.markRead(userDetails.getUserId(), notificationId);
    }

    @Operation(summary = "알림 전체 읽음 처리")
    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        notificationService.markAllRead(userDetails.getUserId());
    }
}
