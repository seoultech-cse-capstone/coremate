package com.coremate.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coremate.backend.auth.event.UserWithdrawnEvent;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
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
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 회원 탈퇴. 소셜 로그인으로만 가입한 계정도 탈퇴할 수 있고, 탈퇴하면 소셜 연결을 지워 같은 소셜 계정으로
 * 다시 가입할 수 있어야 한다. Apple 연결의 refresh token 은 커밋 뒤에 해지하도록 이벤트로 넘긴다.
 */
class AuthServiceWithdrawTest {

    private static final Long USER_ID = 1L;

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserSocialAccountRepository socialAccountRepository = mock(UserSocialAccountRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UserSessionService userSessionService = mock(UserSessionService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private final AuthService authService = new AuthService(userRepository, socialAccountRepository,
            mock(JwtProvider.class), passwordEncoder, userSessionService, eventPublisher,
            mock(SignupEmailVerificationService.class));

    @Test
    void 소셜로만_가입한_계정은_비밀번호_없이_탈퇴하고_소셜_연결을_지운다() {
        User user = User.socialSignup("kakao@example.com", "카카오", "유저123456");
        List<UserSocialAccount> links = List.of(link(SocialProvider.KAKAO, null));
        givenUser(user, links);

        authService.withdraw(USER_ID, null);

        assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        verify(socialAccountRepository).deleteAll(links);
        verify(userSessionService).revokeAllAndAnonymize(USER_ID);
        assertThat(publishedEvent().appleRefreshTokens()).isEmpty();
    }

    @Test
    void Apple_연결의_refresh_token_은_탈퇴_이벤트로_넘긴다() {
        User user = User.socialSignup("abc@privaterelay.appleid.com", null, "유저654321");
        givenUser(user, List.of(link(SocialProvider.APPLE, "apple-refresh"), link(SocialProvider.KAKAO, null)));

        authService.withdraw(USER_ID, null);

        UserWithdrawnEvent event = publishedEvent();
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.appleRefreshTokens()).containsExactly("apple-refresh");
    }

    @Test
    void 비밀번호가_있는_계정은_비밀번호_없이_탈퇴할_수_없다() {
        User user = passwordUser();
        givenUser(user, List.of());

        assertInvalidPassword(() -> authService.withdraw(USER_ID, null));

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(socialAccountRepository, never()).deleteAll(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void 비밀번호가_틀리면_탈퇴할_수_없다() {
        givenUser(passwordUser(), List.of());
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertInvalidPassword(() -> authService.withdraw(USER_ID, "wrong"));
    }

    @Test
    void 비밀번호가_맞으면_탈퇴한다() {
        User user = passwordUser();
        givenUser(user, List.of());
        when(passwordEncoder.matches("P@ssw0rd!", "hashed")).thenReturn(true);

        authService.withdraw(USER_ID, "P@ssw0rd!");

        assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        verify(userSessionService).revokeAllAndAnonymize(USER_ID);
    }

    private void givenUser(User user, List<UserSocialAccount> links) {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(socialAccountRepository.findByUserId(USER_ID)).thenReturn(links);
    }

    private static User passwordUser() {
        return User.builder()
                .email("user@example.com")
                .passwordHash("hashed")
                .name("사용자")
                .nickname("사용자")
                .role(UserRole.USER)
                .build();
    }

    private static UserSocialAccount link(SocialProvider provider, String refreshToken) {
        return UserSocialAccount.builder()
                .userId(USER_ID)
                .provider(provider)
                .providerId(provider.name().toLowerCase() + "-sub")
                .refreshToken(refreshToken)
                .build();
    }

    private UserWithdrawnEvent publishedEvent() {
        ArgumentCaptor<UserWithdrawnEvent> captor = ArgumentCaptor.forClass(UserWithdrawnEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    private static void assertInvalidPassword(Runnable withdraw) {
        assertThatThrownBy(withdraw::run)
                .isInstanceOf(CoreMateException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PASSWORD);
    }
}
