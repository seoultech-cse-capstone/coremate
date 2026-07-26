package com.coremate.backend.auth.service;

import com.coremate.backend.auth.dto.request.LoginRequest;
import com.coremate.backend.auth.dto.request.ReissueTokenRequest;
import com.coremate.backend.auth.dto.request.SignupRequest;
import com.coremate.backend.auth.dto.response.AuthTokenResponse;
import com.coremate.backend.auth.dto.response.LoginResponse;
import com.coremate.backend.auth.dto.response.SignupResponse;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.global.security.JwtProvider;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserRole;
import com.coremate.backend.user.entity.UserStatus;
import com.coremate.backend.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private static final String REFRESH_KEY_PREFIX = "refresh:";

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

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

        String accessToken = jwtProvider.createAccessToken(user.getUserId(), user.getRole());
        String refreshToken = jwtProvider.createRefreshToken(user.getUserId());

        saveRefreshToken(user.getUserId(), refreshToken);
        user.updateLastLoginAt(LocalDateTime.now());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresInSec(jwtProvider.getAccessTokenExpirySec())
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .build();
    }

    public void logout(Long userId, String refreshToken) {
        String stored = redisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + userId);
        if (stored == null || !stored.equals(refreshToken)) {
            throw new CoreMateException(ErrorCode.INVALID_TOKEN);
        }
        redisTemplate.delete(REFRESH_KEY_PREFIX + userId);
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
        redisTemplate.delete(REFRESH_KEY_PREFIX + userId);
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse reissue(ReissueTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtProvider.validateToken(refreshToken)) {
            throw new CoreMateException(ErrorCode.INVALID_TOKEN);
        }

        Long userId = jwtProvider.getUserId(refreshToken);
        String stored = redisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + userId);

        if (stored == null || !stored.equals(refreshToken)) {
            throw new CoreMateException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.USER_NOT_FOUND));

        String newAccessToken = jwtProvider.createAccessToken(user.getUserId(), user.getRole());
        String newRefreshToken = jwtProvider.createRefreshToken(user.getUserId());

        saveRefreshToken(userId, newRefreshToken);

        return AuthTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresInSec(jwtProvider.getAccessTokenExpirySec())
                .build();
    }

    private void saveRefreshToken(Long userId, String refreshToken) {
        redisTemplate.opsForValue().set(
                REFRESH_KEY_PREFIX + userId,
                refreshToken,
                jwtProvider.getRefreshTokenExpirySec(),
                TimeUnit.SECONDS
        );
    }
}
