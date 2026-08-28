package com.coremate.backend.avatar.controller;

import com.coremate.backend.avatar.dto.request.SaveRoomPlacementsRequest;
import com.coremate.backend.avatar.dto.request.SaveRoomPoseRequest;
import com.coremate.backend.avatar.dto.response.RoomResponse;
import com.coremate.backend.avatar.service.RoomService;
import com.coremate.backend.global.config.OpenApiConfig;
import com.coremate.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @Operation(summary = "방 가구 배치 저장",
            description = "사용자의 배치를 통째로 교체한다. 소유·격자 범위·겹침·보유 수량 네 가지를"
                    + " 검증하고, 하나라도 실패하면 기존 배치를 그대로 유지한다.")
    @PutMapping("/placements")
    public RoomResponse savePlacements(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SaveRoomPlacementsRequest request
    ) {
        return roomService.savePlacements(userDetails.getUserId(), request);
    }

    @Operation(summary = "아바타 포즈 저장",
            description = "마지막에 취한 포즈를 남긴다. 저장값은 STAND / SIT_DESK / SLEEP_BED 3종이며"
                    + " 좌표는 저장하지 않는다. 포즈가 실제로 바뀔 때만 갱신 시각이 바뀐다.")
    @PatchMapping("/pose")
    public RoomResponse savePose(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SaveRoomPoseRequest request
    ) {
        return roomService.savePose(userDetails.getUserId(), request);
    }
}
