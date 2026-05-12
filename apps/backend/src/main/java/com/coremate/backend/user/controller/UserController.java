package com.coremate.backend.user.controller;

import com.coremate.backend.user.dto.request.UpdateNicknameRequest;
import com.coremate.backend.user.dto.request.UpdateUserProfileRequest;
import com.coremate.backend.user.dto.response.MyProfileResponse;
import com.coremate.backend.user.dto.response.UserLearningProgressResponse;
import com.coremate.backend.user.dto.response.UserSubjectStatResponse;
import com.coremate.backend.user.dto.response.UserSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User")
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Operation(summary = "내 프로필 조회")
    @GetMapping("/me")
    public MyProfileResponse getMyProfile() {
        return MyProfileResponse.builder()
                .userId(42L)
                .email("user@example.com")
                .nickname("coremate")
                .role("USER")
                .level(5)
                .totalXp(1200)
                .schoolName("School A")
                .schoolEmail("student@school.edu")
                .schoolVerified(false)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now().minusDays(30))
                .lastLoginAt(LocalDateTime.now().minusHours(2))
                .build();
    }

    @Operation(summary = "내 프로필 수정")
    @PutMapping("/me")
    public MyProfileResponse updateMyProfile(@RequestBody UpdateUserProfileRequest request) {
        return MyProfileResponse.builder()
                .userId(42L)
                .email("user@example.com")
                .nickname(request.getNickname() != null ? request.getNickname() : "coremate")
                .role("USER")
                .level(5)
                .totalXp(1200)
                .schoolName(request.getSchoolName())
                .schoolEmail(request.getSchoolEmail())
                .schoolVerified(false)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now().minusDays(30))
                .lastLoginAt(LocalDateTime.now().minusHours(2))
                .build();
    }

    @Operation(summary = "내 닉네임 변경")
    @PatchMapping("/me/nickname")
    public UserSummaryResponse updateNickname(@RequestBody UpdateNicknameRequest request) {
        return UserSummaryResponse.builder()
                .userId(42L)
                .nickname(request.getNickname())
                .level(5)
                .avatarImageUrl("https://cdn.example.com/avatar.png")
                .build();
    }

    @Operation(summary = "내 학습 진행도 조회")
    @GetMapping("/me/progress")
    public UserLearningProgressResponse getMyProgress() {
        return UserLearningProgressResponse.builder()
                .userId(42L)
                .completedConceptCount(12)
                .completedCategoryMockCount(30)
                .completedSubjectMockCount(5)
                .overallAchievementRate("0.75")
                .build();
    }

    @Operation(summary = "내 과목 통계 조회")
    @GetMapping("/me/subject-stats")
    public List<UserSubjectStatResponse> getMySubjectStats() {
        return List.of(UserSubjectStatResponse.builder()
                .subjectId(1L)
                .solvedQuestionCount(120)
                .correctQuestionCount(90)
                .wrongQuestionCount(30)
                .achievementRate("0.75")
                .build());
    }
}
