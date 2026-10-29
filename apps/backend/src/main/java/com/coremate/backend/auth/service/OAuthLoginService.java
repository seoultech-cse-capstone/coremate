package com.coremate.backend.auth.service;

import com.coremate.backend.auth.dto.request.OAuthLoginRequest;
import com.coremate.backend.auth.dto.response.LoginResponse;
import com.coremate.backend.auth.oauth.OAuthClient;
import com.coremate.backend.auth.oauth.OAuthUserInfo;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.global.security.JwtProvider;
import com.coremate.backend.reward.event.UserSignedUpEvent;
import com.coremate.backend.user.entity.SocialProvider;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserSocialAccount;
import com.coremate.backend.user.entity.UserStatus;
import com.coremate.backend.user.repository.UserRepository;
import com.coremate.backend.user.repository.UserSocialAccountRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 소셜 로그인 진입점. provider 토큰을 검증해 사용자를 link-or-create 한 뒤,
 * 세션 등록·토큰 발급은 {@link AuthService#login} 과 동일한 흐름을 따른다(응답도 동일).
 *
 * <p>provider 와의 OAuth HTTP 왕복은 트랜잭션 밖에서 하고, DB 작업(link-or-create·세션·토큰)만
 * 트랜잭션으로 묶는다. 외부 지연 동안 DB 커넥션을 쥐지 않기 위함이다(#321).
 */
@Service
public class OAuthLoginService {

    private final Map<SocialProvider, OAuthClient> clients;
    private final UserRepository userRepository;
    private final UserSocialAccountRepository socialAccountRepository;
    private final JwtProvider jwtProvider;
    private final UserSessionService userSessionService;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    public OAuthLoginService(List<OAuthClient> clients,
                             UserRepository userRepository,
                             UserSocialAccountRepository socialAccountRepository,
                             JwtProvider jwtProvider,
                             UserSessionService userSessionService,
                             ApplicationEventPublisher eventPublisher,
                             TransactionTemplate transactionTemplate) {
        this.clients = clients.stream()
                .collect(Collectors.toMap(OAuthClient::provider, Function.identity()));
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.jwtProvider = jwtProvider;
        this.userSessionService = userSessionService;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
    }

    public LoginResponse login(SocialProvider provider, OAuthLoginRequest request) {
        OAuthClient client = clients.get(provider);
        if (client == null) {
            throw new CoreMateException(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        }

        // 외부 OAuth HTTP 왕복(토큰 교환+userinfo)은 트랜잭션 밖에서 — 이 구간에는 DB 커넥션을 쥐지 않는다.
        OAuthUserInfo userInfo = client.fetch(request);

        // link-or-create·세션 등록·토큰 발급(모두 DB 작업)만 한 트랜잭션으로 묶는다.
        return transactionTemplate.execute(status -> completeLogin(provider, userInfo, request));
    }

    private LoginResponse completeLogin(SocialProvider provider, OAuthUserInfo userInfo, OAuthLoginRequest request) {
        User user = linkOrCreate(provider, userInfo);

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new CoreMateException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        // 이하는 AuthService.login() 의 세션 등록·토큰 발급과 동일하다.
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

    private User linkOrCreate(SocialProvider provider, OAuthUserInfo userInfo) {
        Optional<UserSocialAccount> existingLink =
                socialAccountRepository.findByProviderAndProviderId(provider, userInfo.providerId());
        if (existingLink.isPresent()) {
            // 재방문: 이미 연결된 사용자로 로그인. Apple 은 로그인할 때마다 새 refresh token 을 받으니 바꿔 둔다.
            if (userInfo.providerRefreshToken() != null) {
                existingLink.get().updateRefreshToken(userInfo.providerRefreshToken());
            }
            return userRepository.findById(existingLink.get().getUserId())
                    .orElseThrow(() -> new CoreMateException(ErrorCode.USER_NOT_FOUND));
        }

        User user = resolveUser(userInfo);
        socialAccountRepository.save(UserSocialAccount.builder()
                .userId(user.getUserId())
                .provider(provider)
                .providerId(userInfo.providerId())
                .refreshToken(userInfo.providerRefreshToken())
                .build());
        return user;
    }

    private User resolveUser(OAuthUserInfo userInfo) {
        // 검증된 이메일이 기존 계정과 같으면 그 계정에 연동한다(계정 중복 방지).
        if (userInfo.emailVerified() && userInfo.email() != null) {
            Optional<User> byEmail = userRepository.findByEmail(userInfo.email());
            if (byEmail.isPresent()) {
                return byEmail.get();
            }
        }

        // 신규 소셜 가입. 닉네임은 서버가 생성하고, 스타터팩 지급을 위해 가입 이벤트를 발행한다.
        User user = userRepository.save(
                User.socialSignup(userInfo.email(), userInfo.name(), generateNickname()));
        eventPublisher.publishEvent(new UserSignedUpEvent(user.getUserId()));
        return user;
    }

    private String generateNickname() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String nickname = "유저" + (100000 + ThreadLocalRandom.current().nextInt(900000));
            if (!userRepository.existsByNickname(nickname)) {
                return nickname;
            }
        }
        throw new CoreMateException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private LocalDateTime refreshExpiresAt() {
        return LocalDateTime.now().plusSeconds(jwtProvider.getRefreshTokenExpirySec());
    }
}
