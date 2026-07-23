package com.coremate.backend.friend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coremate.backend.friend.dto.request.PokeFriendRequest;
import com.coremate.backend.friend.dto.request.RespondFriendRequest;
import com.coremate.backend.friend.dto.request.SendFriendRequest;
import com.coremate.backend.friend.dto.response.FriendPokeResponse;
import com.coremate.backend.friend.dto.response.FriendProfileResponse;
import com.coremate.backend.friend.dto.response.FriendRequestResponse;
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
    void pokeCanBeSentAndMarkedReadAfterFriendAccepted() {
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

        assertThat(poke.isRead()).isFalse();
        assertThat(friendService.getReceivedPokes(receiver.getUserId(), true)).hasSize(1);

        FriendPokeResponse readPoke = friendService.markPokeRead(receiver.getUserId(), poke.pokeId());

        assertThat(readPoke.isRead()).isTrue();
        assertThat(readPoke.readAt()).isNotNull();
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
