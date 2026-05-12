package com.coremate.backend.avatar.controller;

import com.coremate.backend.avatar.dto.request.UpdateAvatarProfileRequest;
import com.coremate.backend.avatar.dto.request.UpdateEquippedItemsRequest;
import com.coremate.backend.avatar.dto.response.AvatarEquippedItemResponse;
import com.coremate.backend.avatar.dto.response.AvatarItemResponse;
import com.coremate.backend.avatar.dto.response.AvatarProfileResponse;
import com.coremate.backend.avatar.dto.response.UserAvatarResponse;
import com.coremate.backend.avatar.dto.type.AvatarGenderType;
import com.coremate.backend.avatar.dto.type.AvatarItemType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Avatar")
@RestController
@RequestMapping("/api/avatar")
public class AvatarController {

    @Operation(summary = "내 아바타 조회")
    @GetMapping("/me")
    public UserAvatarResponse getMyAvatar() {
        AvatarProfileResponse profile = sampleProfile();
        return new UserAvatarResponse(42L, profile, sampleItems());
    }

    @Operation(summary = "아바타 프로필 수정")
    @PutMapping("/profile")
    public AvatarProfileResponse updateProfile(@RequestBody UpdateAvatarProfileRequest request) {
        return new AvatarProfileResponse(
                42L,
                request.avatarGender() != null ? request.avatarGender() : AvatarGenderType.OTHER,
                request.baseAvatarType() != null ? request.baseAvatarType() : "BASIC_01",
                sampleEquippedItems(),
                LocalDateTime.now()
        );
    }

    @Operation(summary = "장착 아이템 변경")
    @PutMapping("/equipped-items")
    public AvatarProfileResponse updateEquippedItems(@RequestBody UpdateEquippedItemsRequest request) {
        return sampleProfile();
    }

    private AvatarProfileResponse sampleProfile() {
        return new AvatarProfileResponse(
                42L,
                AvatarGenderType.FEMALE,
                "BASIC_01",
                sampleEquippedItems(),
                LocalDateTime.now()
        );
    }

    private List<AvatarEquippedItemResponse> sampleEquippedItems() {
        return List.of(new AvatarEquippedItemResponse(
                "HEAD",
                100L,
                1L,
                "Blue Hat",
                AvatarItemType.HEAD,
                "RARE",
                "https://cdn.example.com/hat.png"
        ));
    }

    private List<AvatarItemResponse> sampleItems() {
        return List.of(new AvatarItemResponse(
                1L,
                "Blue Hat",
                AvatarItemType.HEAD,
                "HEAD",
                "RARE",
                "https://cdn.example.com/hat.png",
                "A cool blue hat",
                true
        ));
    }
}
