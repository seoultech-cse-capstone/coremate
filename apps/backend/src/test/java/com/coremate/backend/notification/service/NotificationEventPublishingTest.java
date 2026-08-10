package com.coremate.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.coremate.backend.friend.dto.request.PokeFriendRequest;
import com.coremate.backend.friend.dto.request.RespondFriendRequest;
import com.coremate.backend.friend.dto.request.SendFriendRequest;
import com.coremate.backend.friend.dto.response.FriendRequestResponse;
import com.coremate.backend.friend.dto.type.FriendStatus;
import com.coremate.backend.friend.service.FriendService;
import com.coremate.backend.notification.entity.type.NotificationType;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserRole;
import com.coremate.backend.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

// 알림은 커밋 이후에 발송되므로, 롤백되는 테스트 트랜잭션 안에서는 실제 발송이 일어나지 않는다.
// 그래서 "발송 이벤트가 올바른 내용으로 발행되는가"까지를 여기서 검증하고,
// 이벤트를 받은 뒤의 저장·발송 동작은 NotificationSenderTest 가 담당한다.
@SpringBootTest
@Transactional
@RecordApplicationEvents
class NotificationEventPublishingTest {

    @Autowired
    private FriendService friendService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationEvents events;

    @Test
    void pokePublishesNotificationToReceiver() {
        User sender = createUser("evt1@example.com", "evt1");
        User receiver = createUser("evt2@example.com", "evt2");
        becomeFriends(sender, receiver);

        friendService.poke(sender.getUserId(), PokeFriendRequest.builder()
                .toUserId(receiver.getUserId())
                .messageType("POKE")
                .build());

        NotificationCommand command = onlyCommandOf(NotificationType.FRIEND_POKE);
        assertThat(command.userId()).isEqualTo(receiver.getUserId());
        assertThat(command.title()).contains(sender.getNickname());
        assertThat(command.targetId()).isEqualTo(sender.getUserId());
    }

    @Test
    void friendRequestPublishesNotificationToReceiver() {
        User requester = createUser("evt3@example.com", "evt3");
        User receiver = createUser("evt4@example.com", "evt4");

        friendService.sendRequest(requester.getUserId(), SendFriendRequest.builder()
                .receiverId(receiver.getUserId())
                .build());

        NotificationCommand command = onlyCommandOf(NotificationType.FRIEND_REQUEST_RECEIVED);
        assertThat(command.userId()).isEqualTo(receiver.getUserId());
        assertThat(command.body()).contains(requester.getNickname());
    }

    @Test
    void acceptPublishesNotificationToRequester() {
        User requester = createUser("evt5@example.com", "evt5");
        User receiver = createUser("evt6@example.com", "evt6");
        becomeFriends(requester, receiver);

        NotificationCommand command = onlyCommandOf(NotificationType.FRIEND_REQUEST_ACCEPTED);
        assertThat(command.userId()).isEqualTo(requester.getUserId());
        assertThat(command.body()).contains(receiver.getNickname());
    }

    @Test
    void rejectPublishesNoNotification() {
        User requester = createUser("evt7@example.com", "evt7");
        User receiver = createUser("evt8@example.com", "evt8");
        FriendRequestResponse request = friendService.sendRequest(
                requester.getUserId(),
                SendFriendRequest.builder().receiverId(receiver.getUserId()).build());

        friendService.respond(receiver.getUserId(), RespondFriendRequest.builder()
                .friendId(request.friendId())
                .status(FriendStatus.REJECTED)
                .build());

        // 거절당한 사실을 통보받는 건 관계에 도움이 되지 않아 알리지 않는다(FR-NOTI-08).
        assertThat(commandsOf(NotificationType.FRIEND_REQUEST_ACCEPTED)).isEmpty();
    }

    private List<NotificationCommand> commandsOf(NotificationType type) {
        return events.stream(NotificationCommand.class)
                .filter(command -> command.type() == type)
                .toList();
    }

    private NotificationCommand onlyCommandOf(NotificationType type) {
        List<NotificationCommand> commands = commandsOf(type);
        assertThat(commands).hasSize(1);
        return commands.get(0);
    }

    private void becomeFriends(User requester, User receiver) {
        FriendRequestResponse request = friendService.sendRequest(
                requester.getUserId(),
                SendFriendRequest.builder().receiverId(receiver.getUserId()).build());
        friendService.respond(receiver.getUserId(), RespondFriendRequest.builder()
                .friendId(request.friendId())
                .status(FriendStatus.ACCEPTED)
                .build());
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
