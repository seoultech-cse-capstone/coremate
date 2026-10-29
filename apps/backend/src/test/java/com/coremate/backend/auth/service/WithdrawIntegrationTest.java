package com.coremate.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.coremate.backend.user.entity.SocialProvider;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserSocialAccount;
import com.coremate.backend.user.entity.UserStatus;
import com.coremate.backend.user.repository.UserRepository;
import com.coremate.backend.user.repository.UserSocialAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 탈퇴 통합 테스트(H2). 소셜 로그인으로만 가입한 계정이 실제 저장소를 거쳐 탈퇴되고,
 * 소셜 연결 행이 지워져 같은 소셜 계정으로 다시 가입할 수 있는 상태가 되는지 확인한다.
 */
@SpringBootTest
@Transactional
@DisplayName("회원 탈퇴")
class WithdrawIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSocialAccountRepository socialAccountRepository;

    @Test
    @DisplayName("소셜로만 가입한 계정은 비밀번호 없이 탈퇴하고 소셜 연결이 지워진다")
    void socialOnlyAccountWithdrawsWithoutPassword() {
        User user = userRepository.save(User.socialSignup("withdraw-apple@test.com", null, "탈퇴테스트"));
        socialAccountRepository.save(UserSocialAccount.builder()
                .userId(user.getUserId())
                .provider(SocialProvider.APPLE)
                .providerId("apple-withdraw-sub")
                .refreshToken("apple-refresh")
                .build());

        authService.withdraw(user.getUserId(), null);

        assertThat(userRepository.findById(user.getUserId()))
                .get()
                .extracting(User::getStatus)
                .isEqualTo(UserStatus.WITHDRAWN);
        assertThat(socialAccountRepository.findByUserId(user.getUserId())).isEmpty();
        // 연결이 남지 않으니 같은 Apple 계정으로 로그인하면 새로 가입된다.
        assertThat(socialAccountRepository.findByProviderAndProviderId(SocialProvider.APPLE, "apple-withdraw-sub"))
                .isEmpty();
    }
}
