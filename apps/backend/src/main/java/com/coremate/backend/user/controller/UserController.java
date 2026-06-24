package com.coremate.backend.user.controller;

import com.coremate.backend.global.security.CustomUserDetails;
import com.coremate.backend.user.dto.request.UpdateNicknameRequest;
import com.coremate.backend.user.dto.request.UpdateUserProfileRequest;
import com.coremate.backend.user.dto.response.MyProfileResponse;
import com.coremate.backend.user.dto.response.UserLearningProgressResponse;
import com.coremate.backend.user.dto.response.UserSubjectStatResponse;
import com.coremate.backend.user.dto.response.UserSummaryResponse;
import com.coremate.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
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

    @Operation(summary = "내 닉네임 변경")
    @PatchMapping("/me/nickname")
    public UserSummaryResponse updateNickname(@AuthenticationPrincipal CustomUserDetails userDetails,
                                              @Valid @RequestBody UpdateNicknameRequest request) {
        return userService.updateNickname(userDetails.getUserId(), request);
    }

    @Operation(summary = "내 학습 진행도 조회")
    @GetMapping("/me/learning-progress")
    public UserLearningProgressResponse getMyLearningProgress(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return userService.getLearningProgress(userDetails.getUserId());
    }

    @Operation(summary = "내 과목 통계 조회")
    @GetMapping("/me/subject-stats")
    public List<UserSubjectStatResponse> getMySubjectStats(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return userService.getSubjectStats(userDetails.getUserId());
    }
}
