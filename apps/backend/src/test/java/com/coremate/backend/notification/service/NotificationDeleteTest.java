package com.coremate.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.notification.dto.response.NotificationResponse;
import com.coremate.backend.notification.entity.Notification;
import com.coremate.backend.notification.entity.type.NotificationTargetType;
import com.coremate.backend.notification.entity.type.NotificationType;
import com.coremate.backend.notification.repository.NotificationRepository;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserRole;
import com.coremate.backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림함 한 건 삭제(이슈 #249). 본인 알림만 지울 수 있고, 남의 알림과 이미 지운 알림은
 * 읽음 처리와 똑같이 NOTIFICATION_NOT_FOUND 로 막는지 확인한다.
 */
@SpringBootTest
@Transactional
@DisplayName("알림 삭제")
class NotificationDeleteTest {

    @Autowired private NotificationService notificationService;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private UserRepository userRepository;

    @Test
    @DisplayName("본인 알림을 지우면 목록과 안 읽은 개수에서 빠진다")
    void deletesOwnNotification() {
        User me = createUser("noti-del1@example.com", "notidel1");
        Notification target = createNotification(me);
        Notification other = createNotification(me);

        notificationService.deleteNotification(me.getUserId(), target.getNotificationId());

        assertThat(notificationRepository.findById(target.getNotificationId())).isEmpty();
        assertThat(notificationService.getNotifications(me.getUserId()))
                .extracting(NotificationResponse::notificationId)
                .containsExactly(other.getNotificationId());
        assertThat(notificationService.getUnreadCount(me.getUserId())).isEqualTo(1);
    }

    @Test
    @DisplayName("남의 알림은 지울 수 없고 그대로 남는다")
    void cannotDeleteOthersNotification() {
        User owner = createUser("noti-del2@example.com", "notidel2");
        User intruder = createUser("noti-del3@example.com", "notidel3");
        Notification notification = createNotification(owner);

        assertThatThrownBy(() -> notificationService.deleteNotification(
                intruder.getUserId(), notification.getNotificationId()))
                .isInstanceOf(CoreMateException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);

        assertThat(notificationRepository.findById(notification.getNotificationId())).isPresent();
    }

    @Test
    @DisplayName("이미 지운 알림을 다시 지우면 NOTIFICATION_NOT_FOUND 다")
    void deletingTwiceIsNotFound() {
        User me = createUser("noti-del4@example.com", "notidel4");
        Notification notification = createNotification(me);
        notificationService.deleteNotification(me.getUserId(), notification.getNotificationId());

        assertThatThrownBy(() -> notificationService.deleteNotification(
                me.getUserId(), notification.getNotificationId()))
                .isInstanceOf(CoreMateException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    private Notification createNotification(User user) {
        return notificationRepository.save(new Notification(
                user, NotificationType.FRIEND_POKE, "콕 찌르기", "친구가 콕 찔렀어요",
                NotificationTargetType.FRIEND, null));
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
