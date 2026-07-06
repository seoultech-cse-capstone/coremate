package com.coremate.backend.avatar.controller;

import com.coremate.backend.avatar.dto.request.UpdateAvatarProfileRequest;
import com.coremate.backend.avatar.dto.request.UpdateEquippedItemsRequest;
import com.coremate.backend.avatar.dto.response.AvatarProfileResponse;
import com.coremate.backend.avatar.dto.response.UserAvatarResponse;
import com.coremate.backend.avatar.service.AvatarService;
import com.coremate.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Avatar")
@RestController
@RequestMapping("/api/avatar")
@RequiredArgsConstructor
public class AvatarController {

    private final AvatarService avatarService;

    @Operation(summary = "내 아바타 조회")
    @GetMapping("/me")
    public UserAvatarResponse getMyAvatar(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return avatarService.getMyAvatar(userDetails.getUserId());
    }

    @Operation(summary = "아바타 프로필 수정")
    @PutMapping("/profile")
    public AvatarProfileResponse updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateAvatarProfileRequest request
    ) {
        return avatarService.updateProfile(userDetails.getUserId(), request);
    }

    @Operation(summary = "장착 아이템 변경")
    @PutMapping("/equipped-items")
    public AvatarProfileResponse updateEquippedItems(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateEquippedItemsRequest request
    ) {
        return avatarService.updateEquippedItems(userDetails.getUserId(), request);
    }
}
