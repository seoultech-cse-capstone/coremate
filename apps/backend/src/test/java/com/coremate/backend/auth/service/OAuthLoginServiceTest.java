package com.coremate.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coremate.backend.auth.dto.request.OAuthLoginRequest;
import com.coremate.backend.auth.dto.response.LoginResponse;
import com.coremate.backend.auth.oauth.OAuthClient;
import com.coremate.backend.auth.oauth.OAuthUserInfo;
import com.coremate.backend.global.security.JwtProvider;
import com.coremate.backend.user.entity.SocialProvider;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserRole;
import com.coremate.backend.user.entity.UserSocialAccount;
import com.coremate.backend.user.entity.UserStatus;
import com.coremate.backend.user.repository.UserRepository;
import com.coremate.backend.user.repository.UserSocialAccountRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 소셜 로그인에서 외부 OAuth HTTP 호출(client.fetch)이 트랜잭션 밖에서 일어나고,
 * DB 작업(link-or-create·세션·토큰)은 트랜잭션 안에서 수행되는지 확인한다(#321).
 */
class OAuthLoginServiceTest {

    @Test
    void loginFetchesOutsideTransactionAndWrapsDbWork() {
        OAuthClient kakaoClient = mock(OAuthClient.class);
        UserRepository userRepository = mock(UserRepository.class);
        UserSocialAccountRepository socialAccountRepository = mock(UserSocialAccountRepository.class);
        JwtProvider jwtProvider = mock(JwtProvider.class);
        UserSessionService userSessionService = mock(UserSessionService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

        when(kakaoClient.provider()).thenReturn(SocialProvider.KAKAO);
        // 트랜잭션 콜백을 그 자리에서 실행해 동기처럼 검증한다.
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });

        OAuthLoginService service = new OAuthLoginService(
                List.of(kakaoClient), userRepository, socialAccountRepository,
                jwtProvider, userSessionService, eventPublisher, transactionTemplate);

        OAuthLoginRequest request = mock(OAuthLoginRequest.class);
        lenient().when(request.getDeviceId()).thenReturn("device-1");
        lenient().when(request.getDeviceName()).thenReturn("Galaxy");
        lenient().when(request.getPlatform()).thenReturn("ANDROID");
        lenient().when(request.getOsVersion()).thenReturn("14");

        OAuthUserInfo userInfo = new OAuthUserInfo("pid-1", "user@example.com", true, "사용자");
        when(kakaoClient.fetch(request)).thenReturn(userInfo);

        // 이미 연결된 계정으로 재방문 로그인.
        UserSocialAccount link = mock(UserSocialAccount.class);
        when(link.getUserId()).thenReturn(1L);
        when(socialAccountRepository.findByProviderAndProviderId(SocialProvider.KAKAO, "pid-1"))
                .thenReturn(Optional.of(link));
        User user = mock(User.class);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(user.getUserId()).thenReturn(1L);
        when(user.getRole()).thenReturn(UserRole.USER);
        when(user.getNickname()).thenReturn("닉네임");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(userSessionService.registerOnLogin(anyLong(), any(), any(), any(), any()))
                .thenReturn(new UserSessionService.SessionRegistration("device-1", "session-1"));
        when(jwtProvider.createAccessToken(1L, UserRole.USER, "session-1")).thenReturn("access-token");
        when(jwtProvider.createRefreshToken(1L, "session-1")).thenReturn("refresh-token");
        when(jwtProvider.getAccessTokenExpirySec()).thenReturn(3600L);
        when(jwtProvider.getRefreshTokenExpirySec()).thenReturn(1_209_600L);

        LoginResponse response = service.login(SocialProvider.KAKAO, request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getDeviceId()).isEqualTo("device-1");
        // 외부 HTTP 가 호출됐고, DB 작업은 트랜잭션 템플릿으로 감싸였다.
        verify(kakaoClient).fetch(request);
        verify(transactionTemplate).execute(any());
        verify(userSessionService).attachRefreshToken(any(), any(), any());
    }
}
