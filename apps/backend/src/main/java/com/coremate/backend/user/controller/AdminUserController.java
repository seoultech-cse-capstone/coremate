package com.coremate.backend.user.controller;

import com.coremate.backend.auth.service.AuthService;
import com.coremate.backend.global.config.OpenApiConfig;
import com.coremate.backend.global.response.ApiResponse;
import com.coremate.backend.user.dto.response.AdminUserResponse;
import com.coremate.backend.user.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AdminUser", description = "관리자 회원 찾기·탈퇴 처리 API (메일로 온 계정 삭제 요청 처리용)")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AuthService authService;

    @Operation(summary = "회원 찾기", description = "가입 이메일이나 닉네임이 정확히 같은 회원을 찾는다.")
    @GetMapping
    public ApiResponse<List<AdminUserResponse>> searchUsers(@RequestParam String keyword) {
        return ApiResponse.success(adminUserService.search(keyword));
    }

    @Operation(summary = "회원 탈퇴 처리",
            description = "앱에서 탈퇴할 때와 같은 범위로 지운다. 비밀번호는 묻지 않으며, 운영자 계정은 탈퇴시킬 수 없다.")
    @PostMapping("/{userId}/withdraw")
    public ApiResponse<Void> withdrawUser(@PathVariable Long userId) {
        authService.withdrawByAdmin(userId);
        return ApiResponse.success("회원을 탈퇴 처리했습니다.");
    }
}
