package com.coremate.backend.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.user.dto.request.UpdateNicknameRequest;
import com.coremate.backend.user.dto.response.UserSummaryResponse;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 첫 닉네임 무료 설정 통합 테스트(H2, #415). 닉네임 변경은 상점 변경권으로만 하지만(#202),
 * 온보딩에서 계정당 한 번은 코인 없이 정할 수 있어야 한다.
 */
@SpringBootTest
@Transactional
class UserInitialNicknameIntegrationTest {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.socialSignup("initial-nick@test.com", null, "유저100001"));
    }

    @Test
    void 첫_닉네임은_한_번만_무료로_정할_수_있다() {
        assertThat(userService.getMyProfile(user.getUserId()).getInitialNicknameSet()).isFalse();

        UserSummaryResponse response = userService.setInitialNickname(user.getUserId(), request("코어짱"));

        assertThat(response.getNickname()).isEqualTo("코어짱");
        assertThat(userService.getMyProfile(user.getUserId()).getInitialNicknameSet()).isTrue();

        assertThatThrownBy(() -> userService.setInitialNickname(user.getUserId(), request("다른닉")))
                .isInstanceOf(CoreMateException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INITIAL_NICKNAME_ALREADY_SET);
        assertThat(reloadUser().getNickname()).isEqualTo("코어짱");
    }

    @Test
    void 앞뒤_공백은_정리해서_저장한다() {
        userService.setInitialNickname(user.getUserId(), request("  코어  "));

        assertThat(reloadUser().getNickname()).isEqualTo("코어");
    }

    @Test
    void 다른_사람이_쓰는_닉네임이면_거절하고_무료_기회는_남는다() {
        userRepository.save(User.socialSignup("other@test.com", null, "선점닉"));

        assertThatThrownBy(() -> userService.setInitialNickname(user.getUserId(), request("선점닉")))
                .isInstanceOf(CoreMateException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_NICKNAME);
        assertThat(reloadUser().isInitialNicknameSet()).isFalse();
    }

    @Test
    void 지금_닉네임과_같으면_거절하고_무료_기회는_남는다() {
        assertThatThrownBy(() -> userService.setInitialNickname(user.getUserId(), request("유저100001")))
                .isInstanceOf(CoreMateException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NICKNAME_UNCHANGED);
        assertThat(reloadUser().isInitialNicknameSet()).isFalse();
    }

    private UpdateNicknameRequest request(String nickname) {
        return UpdateNicknameRequest.builder().nickname(nickname).build();
    }

    private User reloadUser() {
        return userRepository.findById(user.getUserId()).orElseThrow();
    }
}
