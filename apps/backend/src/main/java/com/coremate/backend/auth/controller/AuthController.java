package com.coremate.backend.auth.controller;

import com.coremate.backend.auth.dto.request.LoginRequest;
import com.coremate.backend.auth.dto.request.LogoutRequest;
import com.coremate.backend.auth.dto.request.ReissueTokenRequest;
import com.coremate.backend.auth.dto.request.SignupRequest;
import com.coremate.backend.auth.dto.response.AuthTokenResponse;
import com.coremate.backend.auth.dto.response.LoginResponse;
import com.coremate.backend.auth.dto.response.SignupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return LoginResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresInSec(3600L)
                .userId(42L)
                .nickname("coremate")
                .build();
    }

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    public SignupResponse signup(@Valid @RequestBody SignupRequest request) {
        return SignupResponse.builder()
                .userId(42L)
                .email(request.getEmail())
                .nickname(request.getNickname())
                .build();
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public AuthTokenResponse logout(@Valid @RequestBody LogoutRequest request) {
        return AuthTokenResponse.builder()
                .accessToken("")
                .refreshToken(request.getRefreshToken())
                .expiresInSec(0L)
                .build();
    }

    @Operation(summary = "토큰 재발급")
    @PostMapping("/reissue")
    public AuthTokenResponse reissue(@Valid @RequestBody ReissueTokenRequest request) {
        return AuthTokenResponse.builder()
                .accessToken("new-access-token")
                .refreshToken(request.getRefreshToken())
                .expiresInSec(3600L)
                .build();
    }
}
