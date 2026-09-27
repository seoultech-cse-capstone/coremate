package com.coremate.backend.auth.service;

import com.coremate.backend.auth.dto.request.SendSignupCodeRequest;
import com.coremate.backend.auth.dto.request.VerifySignupCodeRequest;
import com.coremate.backend.auth.dto.response.SignupCodeResponse;
import com.coremate.backend.auth.dto.response.SignupVerifyResponse;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.global.mail.MailService;
import com.coremate.backend.user.repository.UserRepository;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 회원가입 전 이메일 소유를 확인하는 인증번호 발송/검증 로직.
 * 비밀번호 재설정({@link PasswordResetService})과 동일한 6자리 코드 흐름을 쓰되,
 * 성공 시 재설정 토큰이 아니라 "이메일 인증 완료" 상태를 Redis 에 저장해
 * {@code AuthService.signup} 이 이를 확인/소진하도록 한다.
 */
@Service
@RequiredArgsConstructor
public class SignupEmailVerificationService {

    private static final String CODE_PREFIX = "signup:code:";
    private static final String COOLDOWN_PREFIX = "signup:cooldown:";
    private static final String ATTEMPTS_PREFIX = "signup:attempts:";
    private static final String VERIFIED_PREFIX = "signup:verified:";

    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final MailService mailService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${signup-email.code-ttl-sec}")
    private long codeTtlSec;

    @Value("${signup-email.verified-ttl-sec}")
    private long verifiedTtlSec;

    @Value("${signup-email.resend-cooldown-sec}")
    private long resendCooldownSec;

    @Value("${signup-email.code-length}")
    private int codeLength;

    @Value("${signup-email.max-verify-attempts}")
    private int maxVerifyAttempts;

    public SignupCodeResponse sendCode(SendSignupCodeRequest request) {
        String email = request.getEmail();

        if (userRepository.existsByEmail(email)) {
            throw new CoreMateException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (Boolean.TRUE.equals(redisTemplate.hasKey(COOLDOWN_PREFIX + email))) {
            throw new CoreMateException(ErrorCode.TOO_MANY_VERIFICATION_ATTEMPTS);
        }

        String code = generateCode();
        mailService.sendSignupVerificationCode(email, code);

        redisTemplate.opsForValue().set(CODE_PREFIX + email, code, codeTtlSec, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(COOLDOWN_PREFIX + email, "1", resendCooldownSec, TimeUnit.SECONDS);
        redisTemplate.delete(ATTEMPTS_PREFIX + email);

        return SignupCodeResponse.builder()
                .expiresInSec(codeTtlSec)
                .build();
    }

    public SignupVerifyResponse verifyCode(VerifySignupCodeRequest request) {
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

        // 검증 성공 → 코드/시도 횟수 소진, "인증 완료" 상태 저장(회원가입 시 소진)
        redisTemplate.delete(CODE_PREFIX + email);
        redisTemplate.delete(ATTEMPTS_PREFIX + email);
        redisTemplate.opsForValue().set(VERIFIED_PREFIX + email, "1", verifiedTtlSec, TimeUnit.SECONDS);

        return SignupVerifyResponse.builder()
                .verified(true)
                .expiresInSec(verifiedTtlSec)
                .build();
    }

    /**
     * 회원가입 시 호출. 이메일 인증이 완료된 상태여야 하며, 확인 후 상태를 소진해 재사용을 막는다.
     */
    public void assertVerifiedAndConsume(String email) {
        Boolean verified = redisTemplate.hasKey(VERIFIED_PREFIX + email);
        if (!Boolean.TRUE.equals(verified)) {
            throw new CoreMateException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        redisTemplate.delete(VERIFIED_PREFIX + email);
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(codeLength);
        for (int i = 0; i < codeLength; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }
}
