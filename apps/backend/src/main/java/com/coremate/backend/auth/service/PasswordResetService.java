package com.coremate.backend.auth.service;

import com.coremate.backend.auth.dto.request.ResetPasswordRequest;
import com.coremate.backend.auth.dto.request.SendResetCodeRequest;
import com.coremate.backend.auth.dto.request.VerifyResetCodeRequest;
import com.coremate.backend.auth.dto.response.ResetCodeResponse;
import com.coremate.backend.auth.dto.response.ResetTokenResponse;
import com.coremate.backend.auth.entity.SessionRevokeReason;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.global.mail.MailService;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.repository.UserRepository;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PasswordResetService {

    private static final String CODE_PREFIX = "pw-reset:code:";
    private static final String COOLDOWN_PREFIX = "pw-reset:cooldown:";
    private static final String ATTEMPTS_PREFIX = "pw-reset:attempts:";
    private static final String TOKEN_PREFIX = "pw-reset:token:";

    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final UserSessionService userSessionService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${password-reset.code-ttl-sec}")
    private long codeTtlSec;

    @Value("${password-reset.token-ttl-sec}")
    private long tokenTtlSec;

    @Value("${password-reset.resend-cooldown-sec}")
    private long resendCooldownSec;

    @Value("${password-reset.code-length}")
    private int codeLength;

    @Value("${password-reset.max-verify-attempts}")
    private int maxVerifyAttempts;

    /**
     * 인증번호 발송. 계정 열거 방지를 위해 미가입 이메일이어도 동일한 응답을 반환하고,
     * 실제 발송/저장만 생략한다.
     */
    public ResetCodeResponse sendResetCode(SendResetCodeRequest request) {
        String email = request.getEmail();

        if (userRepository.findByEmail(email).isPresent()) {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(COOLDOWN_PREFIX + email))) {
                throw new CoreMateException(ErrorCode.TOO_MANY_VERIFICATION_ATTEMPTS);
            }

            String code = generateCode();
            // 메일 발송이 성공한 경우에만 코드를 저장한다. (발송 실패 시 orphan 코드/쿨다운을 남기지 않음)
            mailService.sendVerificationCode(email, code);

            redisTemplate.opsForValue().set(CODE_PREFIX + email, code, codeTtlSec, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(COOLDOWN_PREFIX + email, "1", resendCooldownSec, TimeUnit.SECONDS);
            redisTemplate.delete(ATTEMPTS_PREFIX + email);
        }

        return ResetCodeResponse.builder()
                .expiresInSec(codeTtlSec)
                .build();
    }

    /**
     * 인증번호 검증. 성공 시 코드를 소진하고 단기 재설정 토큰을 발급한다.
     */
    public ResetTokenResponse verifyResetCode(VerifyResetCodeRequest request) {
        String email = request.getEmail();
        String storedCode = redisTemplate.opsForValue().get(CODE_PREFIX + email);

        if (storedCode == null) {
            throw new CoreMateException(ErrorCode.EXPIRED_VERIFICATION_CODE);
        }

        if (!storedCode.equals(request.getCode())) {
            Long attempts = redisTemplate.opsForValue().increment(ATTEMPTS_PREFIX + email);
            if (attempts != null && attempts >= maxVerifyAttempts) {
                redisTemplate.delete(CODE_PREFIX + email);
                redisTemplate.delete(ATTEMPTS_PREFIX + email);
                throw new CoreMateException(ErrorCode.TOO_MANY_VERIFICATION_ATTEMPTS);
            }
            throw new CoreMateException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        // 검증 성공 → 코드/시도 횟수 소진, 재설정 토큰 발급
        redisTemplate.delete(CODE_PREFIX + email);
        redisTemplate.delete(ATTEMPTS_PREFIX + email);

        String resetToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(TOKEN_PREFIX + resetToken, email, tokenTtlSec, TimeUnit.SECONDS);

        return ResetTokenResponse.builder()
                .resetToken(resetToken)
                .expiresInSec(tokenTtlSec)
                .build();
    }

    /**
     * 재설정 토큰으로 새 비밀번호를 저장하고, 기존 세션(refresh token)을 무효화한다.
     */
    public void resetPassword(ResetPasswordRequest request) {
        String tokenKey = TOKEN_PREFIX + request.getResetToken();
        String email = redisTemplate.opsForValue().get(tokenKey);

        if (email == null) {
            throw new CoreMateException(ErrorCode.INVALID_RESET_TOKEN);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CoreMateException(ErrorCode.USER_NOT_FOUND));

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));

        redisTemplate.delete(tokenKey);
        // 재설정한 기기까지 포함해 전 기기를 끊는다. 계정 탈취 대응이 목적이라 공격자 세션을
        // 확실히 끊는 쪽을 택한다. 사유가 PASSWORD_RESET 이므로 안내 모달은 뜨지 않는다.
        userSessionService.revokeAll(user.getUserId(), SessionRevokeReason.PASSWORD_RESET);
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(codeLength);
        for (int i = 0; i < codeLength; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }
}
