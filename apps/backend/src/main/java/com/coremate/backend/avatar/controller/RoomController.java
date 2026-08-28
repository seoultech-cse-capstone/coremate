package com.coremate.backend.avatar.controller;

import com.coremate.backend.avatar.dto.response.RoomResponse;
import com.coremate.backend.avatar.service.RoomService;
import com.coremate.backend.global.config.OpenApiConfig;
import com.coremate.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Avatar Room")
@RestController
@RequestMapping("/api/avatar/room")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class RoomController {

    private final RoomService roomService;

    @Operation(summary = "내 방 조회",
            description = "장착 12슬롯, 배치 가구(footprint 포함), 마지막 포즈를 한 번에 내려준다."
                    + " 아바타 좌표는 저장하지 않으므로 반환하지 않는다.")
    @GetMapping
    public RoomResponse getMyRoom(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return roomService.getRoom(userDetails.getUserId());
    }
}
