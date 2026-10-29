package com.coremate.backend.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.coremate.backend.user.dto.response.MyProfileResponse;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 내 프로필 통합 테스트(H2). 소셜 로그인으로 가입한 계정도 그 서비스에서 받은 이메일·이름이
 * 내 프로필에 나와야 회원정보 화면에 보여 줄 수 있다(네이버 로그인 검수의 「제공 정보 활용처」).
 */
@SpringBootTest
@Transactional
class UserProfileIntegrationTest {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;

    @Test
    void 소셜_가입_계정도_받은_이메일과_이름이_내_프로필에_나온다() {
        User user = userRepository.save(User.socialSignup("profile-naver@test.com", "김네이버", "프로필테스트"));

        MyProfileResponse profile = userService.getMyProfile(user.getUserId());

        assertThat(profile.getEmail()).isEqualTo("profile-naver@test.com");
        assertThat(profile.getName()).isEqualTo("김네이버");
        assertThat(profile.getHasPassword()).isFalse();
    }
}
