package com.coremate.backend.auth.service;

import com.coremate.backend.auth.dto.request.LoginRequest;
import com.coremate.backend.auth.dto.request.ReissueTokenRequest;
import com.coremate.backend.auth.dto.request.SignupRequest;
import com.coremate.backend.auth.dto.response.AuthTokenResponse;
import com.coremate.backend.auth.dto.response.LoginResponse;
import com.coremate.backend.auth.dto.response.SignupResponse;
import com.coremate.backend.auth.entity.SessionRevokeReason;
import com.coremate.backend.auth.entity.UserSession;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.reward.event.UserSignedUpEvent;
import com.coremate.backend.global.security.JwtProvider;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserRole;
import com.coremate.backend.user.entity.UserStatus;
import com.coremate.backend.user.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserSessionService userSessionService;
    private final ApplicationEventPublisher eventPublisher;

    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CoreMateException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new CoreMateException(ErrorCode.DUPLICATE_NICKNAME);
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .nickname(request.getNickname())
                .phoneNumber(request.getPhoneNumber())
                .role(UserRole.USER)
                .build();

        user = userRepository.save(user);

        // 스타터팩은 가입이 커밋된 뒤에 지급한다(AccountRewardService).
        // 가입 트랜잭션 안에서 지급하면 보상 실패가 가입까지 되돌릴 수 있고,
        // 새 트랜잭션으로 분리하면 아직 커밋되지 않은 이 사용자를 찾지 못한다.
        eventPublisher.publishEvent(new UserSignedUpEvent(user.getUserId()));

        return SignupResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CoreMateException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new CoreMateException(ErrorCode.INVALID_PASSWORD);
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new CoreMateException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        // 토큰에 sid 를 실어야 하므로 세션 등록이 토큰 발급보다 먼저다.
        UserSessionService.SessionRegistration registration = userSessionService.registerOnLogin(
                user.getUserId(), request.getDeviceId(), request.getDeviceName(),
                request.getPlatform(), request.getOsVersion());

        String accessToken =
                jwtProvider.createAccessToken(user.getUserId(), user.getRole(), registration.sessionId());
        String refreshToken =
                jwtProvider.createRefreshToken(user.getUserId(), registration.sessionId());

        userSessionService.attachRefreshToken(registration.sessionId(), refreshToken, refreshExpiresAt());
        user.updateLastLoginAt(LocalDateTime.now());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresInSec(jwtProvider.getAccessTokenExpirySec())
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .deviceId(registration.deviceId())
                .build();
    }

    /**
     * 현재 기기의 세션만 해지한다. 이미 해지된 기기가 로그아웃을 눌러도 204 로 통과시킨다
     * (다른 기기에 밀려난 사용자가 로그아웃 버튼을 눌렀을 때 401 을 보는 일이 없도록).
     */
    public void logout(Long userId, String sessionId) {
        userSessionService.revokeCurrent(userId, sessionId, SessionRevokeReason.USER_LOGOUT);
    }

    public void withdraw(Long userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new CoreMateException(ErrorCode.ALREADY_WITHDRAWN);
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new CoreMateException(ErrorCode.INVALID_PASSWORD);
        }

        user.withdraw();
        userSessionService.revokeAllAndAnonymize(userId);
    }

    public AuthTokenResponse reissue(ReissueTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtProvider.validateToken(refreshToken)) {
            throw new CoreMateException(ErrorCode.INVALID_TOKEN);
        }

        String sessionId = jwtProvider.getSessionId(refreshToken);
        UserSession session = userSessionService.getForReissue(sessionId, refreshToken);

        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new CoreMateException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new CoreMateException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        // 세션 자체는 유지하고 토큰만 회전한다. sid 가 그대로여야 기기 목록의 같은 행으로 이어진다.
        String newAccessToken = jwtProvider.createAccessToken(user.getUserId(), user.getRole(), sessionId);
        String newRefreshToken = jwtProvider.createRefreshToken(user.getUserId(), sessionId);

        userSessionService.attachRefreshToken(sessionId, newRefreshToken, refreshExpiresAt());

        return AuthTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresInSec(jwtProvider.getAccessTokenExpirySec())
                .build();
    }

    private LocalDateTime refreshExpiresAt() {
        return LocalDateTime.now().plusSeconds(jwtProvider.getRefreshTokenExpirySec());
    }
}
