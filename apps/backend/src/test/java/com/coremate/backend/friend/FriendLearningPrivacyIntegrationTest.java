package com.coremate.backend.friend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coremate.backend.friend.dto.request.RespondFriendRequest;
import com.coremate.backend.friend.dto.request.SendFriendRequest;
import com.coremate.backend.friend.dto.response.FriendProfileResponse;
import com.coremate.backend.friend.dto.response.FriendRequestResponse;
import com.coremate.backend.friend.dto.type.FriendStatus;
import com.coremate.backend.friend.service.FriendService;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.user.dto.request.UpdatePrivacyRequest;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserRole;
import com.coremate.backend.user.repository.UserRepository;
import com.coremate.backend.user.service.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 「친구에게 학습 기록 공개」 설정 통합 테스트(H2). 끄면 친구 프로필에서 과목별 스탯이 빠지고
 * Learning History 는 막히며, 레벨·출석 같은 나머지 정보와 상대 쪽 공개 여부는 그대로인지 확인한다.
 */
@SpringBootTest
@Transactional
@DisplayName("학습 기록 공개 설정")
class FriendLearningPrivacyIntegrationTest {

    private static final long SUBJECT_ID = 9101L;

    @Autowired
    private FriendService friendService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;

    private User me;
    private User friend;

    @BeforeEach
    void seed() {
        // 과목별 스탯은 활성 과목마다 한 줄씩 나온다. 과목이 없으면 비공개가 아니어도 빈 목록이라 하나 넣는다.
        entityManager.createNativeQuery("""
                INSERT INTO subject (subject_id, subject_code, subject_name, sort_order, is_active,
                                     created_at, updated_at)
                VALUES (?1, 'PRIVACYTEST', '공개설정테스트과목', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """).setParameter(1, SUBJECT_ID).executeUpdate();
        me = createUser("privacy-me@test.com", "공개설정나");
        friend = createUser("privacy-friend@test.com", "공개설정친구");
        becomeFriends(me, friend);
    }

    @Test
    @DisplayName("기본값은 공개라 친구가 과목별 스탯과 Learning History 를 볼 수 있다")
    void learningIsPublicByDefault() {
        FriendProfileResponse profile = friendService.getProfile(me.getUserId(), friend.getUserId());

        assertThat(profile.learningPublic()).isTrue();
        assertThat(profile.subjectStats()).extracting("subjectId").contains(SUBJECT_ID);
        assertThat(friendService.getLearningHistory(me.getUserId(), friend.getUserId())).isNotNull();
        assertThat(userService.getMyProfile(friend.getUserId()).getLearningPublic()).isTrue();
    }

    @Test
    @DisplayName("비공개로 바꾸면 친구 프로필에서 과목별 스탯이 빠지고 Learning History 는 막힌다")
    void privateLearningIsHiddenFromFriends() {
        userService.updatePrivacy(friend.getUserId(), privacy(false));

        FriendProfileResponse profile = friendService.getProfile(me.getUserId(), friend.getUserId());
        assertThat(profile.learningPublic()).isFalse();
        assertThat(profile.subjectStats()).isEmpty();
        // 레벨·출석은 공개 범위와 관계없이 보인다.
        assertThat(profile.level()).isEqualTo(friend.getLevel());
        assertThat(profile.attendanceCount()).isZero();
        assertThatThrownBy(() -> friendService.getLearningHistory(me.getUserId(), friend.getUserId()))
                .isInstanceOf(CoreMateException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FRIEND_LEARNING_PRIVATE);
        assertThat(userService.getMyProfile(friend.getUserId()).getLearningPublic()).isFalse();
    }

    @Test
    @DisplayName("내 설정은 상대 쪽 공개 여부에 영향을 주지 않는다")
    void privacyIsPerUser() {
        userService.updatePrivacy(friend.getUserId(), privacy(false));

        FriendProfileResponse myProfileSeenByFriend = friendService.getProfile(friend.getUserId(), me.getUserId());
        assertThat(myProfileSeenByFriend.learningPublic()).isTrue();
        assertThat(friendService.getLearningHistory(friend.getUserId(), me.getUserId())).isNotNull();
    }

    @Test
    @DisplayName("다시 공개하면 친구가 다시 볼 수 있다")
    void republishingShowsLearningAgain() {
        userService.updatePrivacy(friend.getUserId(), privacy(false));

        userService.updatePrivacy(friend.getUserId(), privacy(true));

        FriendProfileResponse profile = friendService.getProfile(me.getUserId(), friend.getUserId());
        assertThat(profile.learningPublic()).isTrue();
        assertThat(profile.subjectStats()).isNotEmpty();
        assertThat(friendService.getLearningHistory(me.getUserId(), friend.getUserId())).isNotNull();
    }

    private UpdatePrivacyRequest privacy(boolean learningPublic) {
        return UpdatePrivacyRequest.builder().learningPublic(learningPublic).build();
    }

    private void becomeFriends(User requester, User receiver) {
        FriendRequestResponse request = friendService.sendRequest(
                requester.getUserId(),
                SendFriendRequest.builder().receiverId(receiver.getUserId()).build()
        );
        friendService.respond(
                receiver.getUserId(),
                RespondFriendRequest.builder()
                        .friendId(request.friendId())
                        .status(FriendStatus.ACCEPTED)
                        .build()
        );
    }

    private User createUser(String email, String nickname) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash("{noop}password")
                .nickname(nickname)
                .role(UserRole.USER)
                .build());
    }
}
