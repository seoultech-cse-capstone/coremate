package com.coremate.backend.auth.controller;

import com.coremate.backend.auth.dto.request.FindEmailRequest;
import com.coremate.backend.auth.dto.request.LoginRequest;
import com.coremate.backend.auth.dto.request.LogoutRequest;
import com.coremate.backend.auth.dto.request.ReissueTokenRequest;
import com.coremate.backend.auth.dto.request.ResetPasswordRequest;
import com.coremate.backend.auth.dto.request.SendResetCodeRequest;
import com.coremate.backend.auth.dto.request.SendSignupCodeRequest;
import com.coremate.backend.auth.dto.request.SignupRequest;
import com.coremate.backend.auth.dto.request.VerifyResetCodeRequest;
import com.coremate.backend.auth.dto.request.VerifySignupCodeRequest;
import com.coremate.backend.auth.dto.request.WithdrawRequest;
import com.coremate.backend.auth.dto.response.AuthTokenResponse;
import com.coremate.backend.auth.dto.response.FindEmailResponse;
import com.coremate.backend.auth.dto.response.LoginResponse;
import com.coremate.backend.auth.dto.response.ResetCodeResponse;
import com.coremate.backend.auth.dto.response.ResetTokenResponse;
import com.coremate.backend.auth.dto.response.SignupCodeResponse;
import com.coremate.backend.auth.dto.response.SignupResponse;
import com.coremate.backend.auth.dto.response.SignupVerifyResponse;
import com.coremate.backend.auth.service.AccountRecoveryService;
import com.coremate.backend.auth.service.AuthService;
import com.coremate.backend.auth.service.PasswordResetService;
import com.coremate.backend.auth.service.SignupEmailVerificationService;
import com.coremate.backend.global.config.OpenApiConfig;
import com.coremate.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AccountRecoveryService accountRecoveryService;
    private final PasswordResetService passwordResetService;
    private final SignupEmailVerificationService signupEmailVerificationService;

    @Operation(summary = "회원가입 이메일 인증번호 발송",
            description = "회원가입할 이메일로 6자리 인증번호를 발송한다. 이미 가입된 이메일이면 실패한다.")
    @PostMapping("/email/send-code")
    public SignupCodeResponse sendSignupCode(@Valid @RequestBody SendSignupCodeRequest request) {
        return signupEmailVerificationService.sendCode(request);
    }

    @Operation(summary = "회원가입 이메일 인증번호 검증",
            description = "인증번호 검증에 성공하면 해당 이메일을 인증 완료 상태로 표시한다. 이후 유효시간 안에 회원가입을 완료해야 한다.")
    @PostMapping("/email/verify-code")
    public SignupVerifyResponse verifySignupCode(@Valid @RequestBody VerifySignupCodeRequest request) {
        return signupEmailVerificationService.verifyCode(request);
    }

    @Operation(summary = "회원가입", description = "이메일 인증이 완료된 이메일만 가입할 수 있다.")
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignupResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "로그아웃", description = "현재 기기의 세션만 해지한다. 이미 해지된 상태여도 204 다.")
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request,
                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        // 해지 대상은 토큰의 세션 식별자로 정한다. 바디의 refreshToken 은 구버전 앱과의 호환을 위해
        // 받기만 하고 쓰지 않는다(와이어 포맷을 깨지 않아 백엔드/모바일 배포 순서 의존성을 줄인다).
        authService.logout(userDetails.getUserId(), userDetails.getSessionId());
    }

    @Operation(summary = "토큰 재발급")
    @PostMapping("/reissue")
    public AuthTokenResponse reissue(@Valid @RequestBody ReissueTokenRequest request) {
        return authService.reissue(request);
    }

    @Operation(summary = "아이디(이메일) 찾기", description = "이름과 휴대폰 번호로 가입된 이메일을 마스킹하여 조회한다.")
    @PostMapping("/find-email")
    public FindEmailResponse findEmail(@Valid @RequestBody FindEmailRequest request) {
        return accountRecoveryService.findEmail(request);
    }

    @Operation(summary = "비밀번호 재설정 인증번호 발송", description = "가입 이메일로 6자리 인증번호를 발송한다.")
    @PostMapping("/password/reset-code")
    public ResetCodeResponse sendResetCode(@Valid @RequestBody SendResetCodeRequest request) {
        return passwordResetService.sendResetCode(request);
    }

    @Operation(summary = "비밀번호 재설정 인증번호 검증", description = "인증번호 검증에 성공하면 재설정 토큰을 발급한다.")
    @PostMapping("/password/verify-code")
    public ResetTokenResponse verifyResetCode(@Valid @RequestBody VerifyResetCodeRequest request) {
        return passwordResetService.verifyResetCode(request);
    }

    @Operation(summary = "비밀번호 재설정", description = "재설정 토큰과 새 비밀번호로 비밀번호를 변경한다.")
    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
    }

    @Operation(summary = "회원 탈퇴", description = "본인 확인(비밀번호) 후 계정을 탈퇴 처리하고 세션을 무효화한다.")
    @PostMapping("/withdraw")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@AuthenticationPrincipal CustomUserDetails userDetails,
                         @Valid @RequestBody WithdrawRequest request) {
        authService.withdraw(userDetails.getUserId(), request.getPassword());
    }
}
