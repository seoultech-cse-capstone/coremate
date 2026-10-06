package com.coremate.backend.friend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coremate.backend.friend.dto.request.RespondFriendRequest;
import com.coremate.backend.friend.dto.request.SendFriendRequest;
import com.coremate.backend.friend.dto.response.BlockedUserResponse;
import com.coremate.backend.friend.dto.response.FriendRequestResponse;
import com.coremate.backend.friend.dto.response.FriendSearchUserResponse;
import com.coremate.backend.friend.dto.type.FriendStatus;
import com.coremate.backend.friend.service.FriendService;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserRole;
import com.coremate.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 친구 차단 통합 테스트(H2). 차단하면 관계가 끊기고, 차단당한 쪽은 차단한 사람을 찾거나
 * 요청할 수 없으며, 차단은 차단한 사람만 풀 수 있는지 확인한다.
 */
@SpringBootTest
@Transactional
@DisplayName("친구 차단")
class FriendBlockIntegrationTest {

    @Autowired
    private FriendService friendService;
    @Autowired
    private UserRepository userRepository;

    private User me;
    private User other;

    @BeforeEach
    void seed() {
        me = createUser("block-me@test.com", "차단하는나");
        other = createUser("block-other@test.com", "차단당할친구");
    }

    @Test
    @DisplayName("친구를 차단하면 양쪽 친구 목록에서 사라지고 내 차단 목록에 보인다")
    void blockingEndsFriendship() {
        becomeFriends(me, other);

        friendService.block(me.getUserId(), other.getUserId());

        assertThat(friendService.getFriends(me.getUserId())).isEmpty();
        assertThat(friendService.getFriends(other.getUserId())).isEmpty();
        assertThat(friendService.getBlockedUsers(me.getUserId()))
                .extracting(BlockedUserResponse::userId)
                .containsExactly(other.getUserId());
        assertThat(friendService.getBlockedUsers(other.getUserId())).isEmpty();
        // 프로필·콕 찌르기는 수락된 관계만 되므로 함께 막힌다.
        assertThatThrownBy(() -> friendService.getProfile(other.getUserId(), me.getUserId()))
                .isInstanceOf(CoreMateException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FRIEND_RELATION_REQUIRED);
    }

    @Test
    @DisplayName("차단당한 쪽은 검색에서 나를 찾지 못하고, 요청을 보내면 없는 사용자로 막힌다")
    void blockedUserCannotFindOrRequest() {
        friendService.block(me.getUserId(), other.getUserId());

        assertThat(friendService.searchUsers(other.getUserId(), "차단하는")).isEmpty();
        assertThatThrownBy(() -> friendService.sendRequest(other.getUserId(), requestTo(me)))
                .isInstanceOf(CoreMateException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("차단한 쪽 검색에는 차단함으로 보이고, 풀기 전에는 요청할 수 없다")
    void blockerSeesBlockedAndMustUnblockFirst() {
        friendService.block(me.getUserId(), other.getUserId());

        FriendSearchUserResponse found = friendService.searchUsers(me.getUserId(), "차단당할").get(0);
        assertThat(found.relationshipStatus()).isEqualTo(FriendStatus.BLOCKED);
        assertThatThrownBy(() -> friendService.sendRequest(me.getUserId(), requestTo(other)))
                .isInstanceOf(CoreMateException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FRIEND_BLOCKED_BY_ME);
    }

    @Test
    @DisplayName("차단당한 쪽은 친구 삭제나 차단 해제로 차단을 풀 수 없다")
    void blockedUserCannotRemoveBlock() {
        friendService.block(me.getUserId(), other.getUserId());

        assertThatThrownBy(() -> friendService.delete(other.getUserId(), me.getUserId()))
                .isInstanceOf(CoreMateException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FRIEND_NOT_FOUND);
        assertThatThrownBy(() -> friendService.unblock(other.getUserId(), me.getUserId()))
                .isInstanceOf(CoreMateException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FRIEND_BLOCK_NOT_FOUND);
        assertThat(friendService.getBlockedUsers(me.getUserId())).hasSize(1);
    }

    @Test
    @DisplayName("차단을 풀면 관계가 없어져 다시 요청할 수 있다")
    void unblockAllowsRequestAgain() {
        friendService.block(me.getUserId(), other.getUserId());

        friendService.unblock(me.getUserId(), other.getUserId());

        assertThat(friendService.getBlockedUsers(me.getUserId())).isEmpty();
        FriendRequestResponse request = friendService.sendRequest(other.getUserId(), requestTo(me));
        assertThat(request.status()).isEqualTo(FriendStatus.REQUESTED);
    }

    @Test
    @DisplayName("상대가 이미 나를 차단했으면 내 차단은 그 차단을 덮어쓰지 않는다")
    void blockingDoesNotOverrideOthersBlock() {
        friendService.block(other.getUserId(), me.getUserId());

        friendService.block(me.getUserId(), other.getUserId());

        assertThat(friendService.getBlockedUsers(other.getUserId()))
                .extracting(BlockedUserResponse::userId)
                .containsExactly(me.getUserId());
        assertThat(friendService.getBlockedUsers(me.getUserId())).isEmpty();
    }

    private SendFriendRequest requestTo(User receiver) {
        return SendFriendRequest.builder().receiverId(receiver.getUserId()).build();
    }

    private void becomeFriends(User requester, User receiver) {
        FriendRequestResponse request = friendService.sendRequest(requester.getUserId(), requestTo(receiver));
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
