package com.coremate.backend.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coremate.backend.auth.service.UserSessionService;
import com.coremate.backend.global.security.JwtProvider;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * DELETE /api/notifications/{notificationId} 의 응답 상태를 고정한다(이슈 #249).
 * 앱은 204 면 목록에서 지운 채로 두고, 그 외에는 지웠던 항목을 되돌린다.
 *
 * <p>실제 인증 필터를 타도록 기기 세션을 만들고 액세스 토큰을 발급해 호출한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("알림 삭제 API")
class NotificationDeleteApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private UserSessionService userSessionService;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private UserRepository userRepository;

    @Test
    @DisplayName("본인 알림은 204 로 지워진다")
    void deletesOwnNotification() throws Exception {
        User me = createUser("noti-api1@example.com", "notiapi1");
        Notification notification = createNotification(me);

        mockMvc.perform(delete("/api/notifications/{id}", notification.getNotificationId())
                        .header("Authorization", "Bearer " + accessTokenOf(me)))
                .andExpect(status().isNoContent());

        assertThat(notificationRepository.findById(notification.getNotificationId())).isEmpty();
    }

    @Test
    @DisplayName("남의 알림은 404 NOTIFICATION_NOT_FOUND 다")
    void othersNotificationIsNotFound() throws Exception {
        User owner = createUser("noti-api2@example.com", "notiapi2");
        User intruder = createUser("noti-api3@example.com", "notiapi3");
        Notification notification = createNotification(owner);

        mockMvc.perform(delete("/api/notifications/{id}", notification.getNotificationId())
                        .header("Authorization", "Bearer " + accessTokenOf(intruder)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));

        assertThat(notificationRepository.findById(notification.getNotificationId())).isPresent();
    }

    @Test
    @DisplayName("토큰 없이 부르면 401 이다")
    void requiresAuthentication() throws Exception {
        User owner = createUser("noti-api4@example.com", "notiapi4");
        Notification notification = createNotification(owner);

        mockMvc.perform(delete("/api/notifications/{id}", notification.getNotificationId()))
                .andExpect(status().isUnauthorized());

        assertThat(notificationRepository.findById(notification.getNotificationId())).isPresent();
    }

    private String accessTokenOf(User user) {
        String sessionId = userSessionService
                .registerOnLogin(user.getUserId(), "device-" + user.getUserId(), "iPhone", "IOS", "18.0")
                .sessionId();
        return jwtProvider.createAccessToken(user.getUserId(), UserRole.USER, sessionId);
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
