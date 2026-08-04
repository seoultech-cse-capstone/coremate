package com.coremate.backend.auth.controller;

import com.coremate.backend.auth.dto.response.SessionItem;
import com.coremate.backend.auth.dto.response.SessionListResponse;
import com.coremate.backend.auth.service.UserSessionService;
import com.coremate.backend.global.config.OpenApiConfig;
import com.coremate.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth/sessions")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class SessionController {

    private final UserSessionService userSessionService;

    @Operation(summary = "로그인된 기기 목록 조회",
            description = "본인 계정으로 로그인된 기기를 마지막 접속 순으로 반환한다. "
                    + "current 는 서버가 토큰의 세션 식별자와 비교해 계산한다.")
    @GetMapping
    public SessionListResponse getSessions(@AuthenticationPrincipal CustomUserDetails userDetails) {
        String currentSessionId = userDetails.getSessionId();
        return SessionListResponse.builder()
                .sessions(userSessionService.listActive(userDetails.getUserId(), currentSessionId).stream()
                        .map(session -> SessionItem.of(session, currentSessionId))
                        .toList())
                .build();
    }

    @Operation(summary = "특정 기기 로그아웃(원격 해지)",
            description = "없는 세션·남의 세션·이미 해지된 세션은 모두 204 로 통과시킨다. "
                    + "현재 사용 중인 기기는 해지할 수 없다.")
    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeSession(@AuthenticationPrincipal CustomUserDetails userDetails,
                              @PathVariable String sessionId) {
        userSessionService.revokeBySessionId(userDetails.getUserId(), userDetails.getSessionId(), sessionId);
    }

    @Operation(summary = "다른 모든 기기에서 로그아웃",
            description = "현재 기기를 제외한 본인의 모든 기기 세션을 해지한다.")
    @PostMapping("/revoke-others")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeOtherSessions(@AuthenticationPrincipal CustomUserDetails userDetails) {
        userSessionService.revokeOthers(userDetails.getUserId(), userDetails.getSessionId());
    }
}
