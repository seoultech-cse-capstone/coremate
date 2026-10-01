package com.coremate.backend.friend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coremate.backend.friend.dto.request.PokeFriendRequest;
import com.coremate.backend.friend.dto.request.RespondFriendRequest;
import com.coremate.backend.friend.dto.request.SendFriendRequest;
import com.coremate.backend.friend.dto.response.FriendPokeResponse;
import com.coremate.backend.friend.dto.response.FriendProfileResponse;
import com.coremate.backend.friend.dto.response.FriendRequestResponse;
import com.coremate.backend.friend.dto.response.FriendSearchUserResponse;
import com.coremate.backend.friend.dto.type.FriendStatus;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserRole;
import com.coremate.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class FriendServiceTest {

    @Autowired
    private FriendService friendService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void sendRequestCreatesRequestedRelationAndBlocksDuplicatePair() {
        User requester = createUser("alice1@example.com", "alice1");
        User receiver = createUser("bob1@example.com", "bob1");

        FriendRequestResponse response = friendService.sendRequest(
                requester.getUserId(),
                SendFriendRequest.builder().receiverId(receiver.getUserId()).build()
        );

        assertThat(response.status()).isEqualTo(FriendStatus.REQUESTED);
        assertThat(response.requesterId()).isEqualTo(requester.getUserId());
        assertThat(response.receiverId()).isEqualTo(receiver.getUserId());

        // 같은 사용자쌍은 역방향 요청도 중복으로 막는다.
        assertThatThrownBy(() -> friendService.sendRequest(
                receiver.getUserId(),
                SendFriendRequest.builder().receiverId(requester.getUserId()).build()
        ))
                .isInstanceOf(CoreMateException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FRIEND_REQUEST_DUPLICATED);
    }

    @Test
    void onlyReceiverCanAcceptRequestAndThenProfileIsVisible() {
        User requester = createUser("alice2@example.com", "alice2");
        User receiver = createUser("bob2@example.com", "bob2");
        FriendRequestResponse request = friendService.sendRequest(
                requester.getUserId(),
                SendFriendRequest.builder().receiverId(receiver.getUserId()).build()
        );

        assertThatThrownBy(() -> friendService.respond(
                requester.getUserId(),
                RespondFriendRequest.builder()
                        .friendId(request.friendId())
                        .status(FriendStatus.ACCEPTED)
                        .build()
        ))
                .isInstanceOf(CoreMateException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FRIEND_REQUEST_NOT_RECEIVED);

        friendService.respond(
                receiver.getUserId(),
                RespondFriendRequest.builder()
                        .friendId(request.friendId())
                        .status(FriendStatus.ACCEPTED)
                        .build()
        );

        FriendProfileResponse profile = friendService.getProfile(requester.getUserId(), receiver.getUserId());

        assertThat(profile.userId()).isEqualTo(receiver.getUserId());
        assertThat(profile.nickname()).isEqualTo(receiver.getNickname());
        assertThat(profile.avatarProfile()).isNotNull();
    }

    @Test
    void profileAndPokeRequireAcceptedFriendRelation() {
        User requester = createUser("alice3@example.com", "alice3");
        User receiver = createUser("bob3@example.com", "bob3");

        assertThatThrownBy(() -> friendService.getProfile(requester.getUserId(), receiver.getUserId()))
                .isInstanceOf(CoreMateException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FRIEND_RELATION_REQUIRED);

        assertThatThrownBy(() -> friendService.poke(
                requester.getUserId(),
                PokeFriendRequest.builder()
                        .toUserId(receiver.getUserId())
                        .messageType("POKE")
                        .build()
        ))
                .isInstanceOf(CoreMateException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FRIEND_RELATION_REQUIRED);
    }

    @Test
    void pokeCanBeSentAfterFriendAccepted() {
        User requester = createUser("alice4@example.com", "alice4");
        User receiver = createUser("bob4@example.com", "bob4");
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

        FriendPokeResponse poke = friendService.poke(
                requester.getUserId(),
                PokeFriendRequest.builder()
                        .toUserId(receiver.getUserId())
                        .messageType("POKE")
                        .build()
        );

        assertThat(poke.pokeId()).isNotNull();
        assertThat(poke.fromUserId()).isEqualTo(requester.getUserId());
        assertThat(poke.toUserId()).isEqualTo(receiver.getUserId());
    }

    @Test
    void pokeIsBlockedAfterThreeTimesToSameFriend() {
        User sender = createUser("alice5@example.com", "alice5");
        User friend = createUser("bob5@example.com", "bob5");
        becomeFriends(sender, friend);

        for (int i = 0; i < 3; i++) {
            friendService.poke(sender.getUserId(), pokeRequest(friend.getUserId()));
        }

        assertThatThrownBy(() -> friendService.poke(sender.getUserId(), pokeRequest(friend.getUserId())))
                .isInstanceOf(CoreMateException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FRIEND_POKE_FRIEND_LIMIT_EXCEEDED);
    }

    @Test
    void pokeIsBlockedAfterDailyTotalLimitAcrossFriends() {
        User sender = createUser("alice6@example.com", "alice6");

        // 전체 한도 30 = 친구 10명 × 3회. 친구당 한도(3)에 먼저 걸리지 않도록 나눠 보낸다.
        for (int i = 0; i < 10; i++) {
            User friend = createUser("bob6" + i + "@example.com", "bob6" + i);
            becomeFriends(sender, friend);
            for (int j = 0; j < 3; j++) {
                friendService.poke(sender.getUserId(), pokeRequest(friend.getUserId()));
            }
        }

        // 한 번도 찌르지 않은 친구여도 그날 총량을 다 썼으면 막힌다.
        User freshFriend = createUser("bob6x@example.com", "bob6x");
        becomeFriends(sender, freshFriend);

        assertThatThrownBy(() -> friendService.poke(sender.getUserId(), pokeRequest(freshFriend.getUserId())))
                .isInstanceOf(CoreMateException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FRIEND_POKE_DAILY_LIMIT_EXCEEDED);
    }

    @Test
    void rejectedRequesterSeesPendingAndCannotRequestAgainButRejecterCan() {
        User requester = createUser("alice7@example.com", "alice7");
        User receiver = createUser("bob7@example.com", "bob7");
        FriendRequestResponse request = friendService.sendRequest(
                requester.getUserId(),
                SendFriendRequest.builder().receiverId(receiver.getUserId()).build()
        );
        friendService.respond(
                receiver.getUserId(),
                RespondFriendRequest.builder()
                        .friendId(request.friendId())
                        .status(FriendStatus.REJECTED)
                        .build()
        );

        // 거절당한 쪽에는 요청이 계속 대기 중으로 보이고, 다시 보내면 중복 요청으로 막힌다.
        FriendSearchUserResponse seenByRequester = searchOne(requester, "bob7");
        assertThat(seenByRequester.relationshipStatus()).isEqualTo(FriendStatus.REQUESTED);
        assertThat(seenByRequester.direction()).isEqualTo("SENT");
        assertThatThrownBy(() -> friendService.sendRequest(
                requester.getUserId(),
                SendFriendRequest.builder().receiverId(receiver.getUserId()).build()
        ))
                .isInstanceOf(CoreMateException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FRIEND_REQUEST_DUPLICATED);

        // 거절한 쪽은 거절 상태를 그대로 보고, 마음을 바꾸면 먼저 요청할 수 있다.
        FriendSearchUserResponse seenByReceiver = searchOne(receiver, "alice7");
        assertThat(seenByReceiver.relationshipStatus()).isEqualTo(FriendStatus.REJECTED);
        assertThat(seenByReceiver.direction()).isEqualTo("RECEIVED");

        FriendRequestResponse reopened = friendService.sendRequest(
                receiver.getUserId(),
                SendFriendRequest.builder().receiverId(requester.getUserId()).build()
        );
        assertThat(reopened.status()).isEqualTo(FriendStatus.REQUESTED);
        assertThat(reopened.requesterId()).isEqualTo(receiver.getUserId());
        assertThat(searchOne(requester, "bob7").direction()).isEqualTo("RECEIVED");
    }

    @Test
    void userSearchMatchesNicknameButNotEmail() {
        User searcher = createUser("searcher8@example.com", "searcher8");
        createUser("hidden-mail8@example.com", "carolSearch8");

        assertThat(friendService.searchUsers(searcher.getUserId(), "hidden-mail8")).isEmpty();
        assertThat(friendService.searchUsers(searcher.getUserId(), "carolsearch"))
                .extracting(FriendSearchUserResponse::nickname)
                .contains("carolSearch8");
    }

    private FriendSearchUserResponse searchOne(User searcher, String nickname) {
        return friendService.searchUsers(searcher.getUserId(), nickname).stream()
                .filter(user -> user.nickname().equals(nickname))
                .findFirst()
                .orElseThrow();
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

    private PokeFriendRequest pokeRequest(Long toUserId) {
        return PokeFriendRequest.builder().toUserId(toUserId).messageType("POKE").build();
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
