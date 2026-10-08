package com.coremate.backend.user.controller;

import com.coremate.backend.global.config.OpenApiConfig;
import com.coremate.backend.global.security.CustomUserDetails;
import com.coremate.backend.user.dto.request.ChangePasswordRequest;
import com.coremate.backend.user.dto.request.UpdateNicknameRequest;
import com.coremate.backend.user.dto.request.UpdatePrivacyRequest;
import com.coremate.backend.user.dto.request.UpdateUserProfileRequest;
import com.coremate.backend.user.dto.response.MyProfileResponse;
import com.coremate.backend.user.dto.response.UserSubjectStatResponse;
import com.coremate.backend.user.dto.response.UserSummaryResponse;
import com.coremate.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 프로필 조회")
    @GetMapping("/me")
    public MyProfileResponse getMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return userService.getMyProfile(userDetails.getUserId());
    }

    @Operation(summary = "내 프로필 수정")
    @PutMapping("/me")
    public MyProfileResponse updateMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails,
                                             @Valid @RequestBody UpdateUserProfileRequest request) {
        return userService.updateMyProfile(userDetails.getUserId(), request);
    }

    // 닉네임 무료 변경 경로 폐쇄 — 닉네임 변경권(상점)으로만 바꾸도록 이전한다. (#202)
    /*
    @Operation(summary = "내 닉네임 변경")
    @PatchMapping("/me/nickname")
    public UserSummaryResponse updateNickname(@AuthenticationPrincipal CustomUserDetails userDetails,
                                              @Valid @RequestBody UpdateNicknameRequest request) {
        return userService.updateNickname(userDetails.getUserId(), request);
    }
    */

    @Operation(summary = "내 비밀번호 변경",
            description = "로그인 상태에서 현재 비밀번호를 확인한 뒤 새 비밀번호로 변경한다. "
                    + "성공하면 지금 기기만 남기고 다른 기기는 모두 로그아웃된다.")
    @PatchMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal CustomUserDetails userDetails,
                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userDetails.getUserId(), userDetails.getSessionId(), request);
    }

    @Operation(summary = "내 공개 범위 변경",
            description = "친구에게 학습 기록(과목별 스탯·Learning History)을 보여줄지 정한다. "
                    + "레벨·출석·아바타·방은 이 설정과 관계없이 친구에게 보인다.")
    @PatchMapping("/me/privacy")
    public MyProfileResponse updatePrivacy(@AuthenticationPrincipal CustomUserDetails userDetails,
                                           @Valid @RequestBody UpdatePrivacyRequest request) {
        return userService.updatePrivacy(userDetails.getUserId(), request);
    }

    @Operation(summary = "내 과목 통계 조회")
    @GetMapping("/me/subject-stats")
    public List<UserSubjectStatResponse> getMySubjectStats(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return userService.getSubjectStats(userDetails.getUserId());
    }
}
