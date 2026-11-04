package com.coremate.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.coremate.backend.auth.entity.UserSession;
import com.coremate.backend.auth.repository.UserSessionRepository;
import com.coremate.backend.friend.entity.Friend;
import com.coremate.backend.friend.entity.FriendPoke;
import com.coremate.backend.friend.repository.FriendPokeRepository;
import com.coremate.backend.friend.repository.FriendRepository;
import com.coremate.backend.notification.entity.DeviceToken;
import com.coremate.backend.notification.entity.Notification;
import com.coremate.backend.notification.entity.NotificationSendLog;
import com.coremate.backend.notification.entity.UserNotificationSetting;
import com.coremate.backend.notification.entity.type.DevicePlatform;
import com.coremate.backend.notification.entity.type.NotificationCategory;
import com.coremate.backend.notification.entity.type.NotificationTargetType;
import com.coremate.backend.notification.entity.type.NotificationType;
import com.coremate.backend.notification.repository.DeviceTokenRepository;
import com.coremate.backend.notification.repository.NotificationRepository;
import com.coremate.backend.notification.repository.NotificationSendLogRepository;
import com.coremate.backend.notification.repository.UserNotificationSettingRepository;
import com.coremate.backend.support.entity.BugReport;
import com.coremate.backend.support.entity.Inquiry;
import com.coremate.backend.support.entity.type.InquiryCategory;
import com.coremate.backend.support.repository.BugReportRepository;
import com.coremate.backend.support.repository.InquiryRepository;
import com.coremate.backend.user.dto.response.AdminUserResponse;
import com.coremate.backend.user.entity.SocialProvider;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserSocialAccount;
import com.coremate.backend.user.entity.UserStatus;
import com.coremate.backend.user.repository.UserRepository;
import com.coremate.backend.user.repository.UserSocialAccountRepository;
import com.coremate.backend.user.service.AdminUserService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 탈퇴 통합 테스트(H2). 실제 저장소를 거쳐 탈퇴되고, 계정 삭제 안내 페이지에 「바로 삭제」라고 적은 데이터가
 * 지워지는지, 다른 회원의 데이터는 그대로 남는지 확인한다.
 */
@SpringBootTest
@Transactional
@DisplayName("회원 탈퇴")
class WithdrawIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSocialAccountRepository socialAccountRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationSendLogRepository notificationSendLogRepository;

    @Autowired
    private UserNotificationSettingRepository userNotificationSettingRepository;

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private BugReportRepository bugReportRepository;

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private FriendPokeRepository friendPokeRepository;

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

    @Test
    @DisplayName("탈퇴하면 기기·알림·메모·문의·친구 데이터를 지우고, 다른 회원끼리의 데이터는 남긴다")
    void withdrawalDeletesPersonalDataButKeepsOthers() {
        User me = userRepository.save(User.socialSignup("withdraw-me@test.com", null, "탈퇴할회원"));
        User friend = userRepository.save(User.socialSignup("withdraw-friend@test.com", null, "남는친구"));
        User other = userRepository.save(User.socialSignup("withdraw-other@test.com", null, "다른회원"));
        Long myId = me.getUserId();

        saveSession(myId, "device-me");
        saveSession(friend.getUserId(), "device-friend");
        deviceTokenRepository.save(new DeviceToken(me, "fcm-me", DevicePlatform.ANDROID, "device-me"));
        deviceTokenRepository.save(new DeviceToken(friend, "fcm-friend", DevicePlatform.ANDROID, "device-friend"));
        notificationSendLogRepository.save(NotificationSendLog.success(1L, myId, NotificationType.FRIEND_POKE, 1L));
        notificationSendLogRepository.save(
                NotificationSendLog.success(2L, friend.getUserId(), NotificationType.FRIEND_POKE, 2L));
        userNotificationSettingRepository.save(new UserNotificationSetting(me, NotificationCategory.FRIEND, false));

        notificationRepository.save(poke(me, friend));
        // 친구 알림함에 남은 「탈퇴할회원님이 콕 찔렀어요」도 지워져야 한다.
        notificationRepository.save(poke(friend, me));
        notificationRepository.save(poke(friend, other));

        inquiryRepository.save(new Inquiry(me, InquiryCategory.HOME, "문의 제목", "문의 본문"));
        bugReportRepository.save(new BugReport(me, "버그 신고 본문"));

        friendRepository.save(new Friend(me, friend));
        friendRepository.save(new Friend(other, me));
        friendRepository.save(new Friend(friend, other));
        friendPokeRepository.save(new FriendPoke(me, friend, "POKE"));
        friendPokeRepository.save(new FriendPoke(friend, me, "POKE"));
        friendPokeRepository.save(new FriendPoke(friend, other, "POKE"));

        authService.withdraw(myId, null);

        assertThat(userRepository.findById(myId)).get().extracting(User::getStatus).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(userSessionRepository.findByUserId(myId)).isEmpty();
        assertThat(userSessionRepository.findByUserId(friend.getUserId())).hasSize(1);
        assertThat(deviceTokenRepository.findAll()).extracting(DeviceToken::getToken)
                .doesNotContain("fcm-me").contains("fcm-friend");
        assertThat(notificationSendLogRepository.findAll()).extracting(NotificationSendLog::getUserId)
                .doesNotContain(myId).contains(friend.getUserId());
        assertThat(userNotificationSettingRepository.findByUser_UserId(myId)).isEmpty();

        assertThat(notificationRepository.findAll())
                .filteredOn(n -> n.getUser().getUserId().equals(myId) || myId.equals(n.getTargetId()))
                .isEmpty();
        assertThat(notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(friend.getUserId(),
                Pageable.unpaged()))
                .extracting(Notification::getTargetId)
                .containsExactly(other.getUserId());

        assertThat(inquiryRepository.findByUserUserIdOrderByCreatedAtDesc(myId)).isEmpty();
        assertThat(bugReportRepository.findAll()).filteredOn(r -> r.getUser().getUserId().equals(myId)).isEmpty();

        assertThat(friendRepository.findAll())
                .filteredOn(f -> f.getRequester().getUserId().equals(myId) || f.getReceiver().getUserId().equals(myId))
                .isEmpty();
        assertThat(friendRepository.findByUserLowIdAndUserHighId(
                Math.min(friend.getUserId(), other.getUserId()), Math.max(friend.getUserId(), other.getUserId())))
                .isPresent();
        assertThat(friendPokeRepository.findAll())
                .filteredOn(p -> p.getFromUser().getUserId().equals(myId) || p.getToUser().getUserId().equals(myId))
                .isEmpty();
        assertThat(friendPokeRepository.findAll())
                .filteredOn(p -> p.getFromUser().getUserId().equals(friend.getUserId()))
                .extracting(p -> p.getToUser().getUserId())
                .containsExactly(other.getUserId());
    }

    @Test
    @DisplayName("운영자는 메일로 받은 요청을 가입 이메일로 찾아 비밀번호 없이 탈퇴시킨다")
    void adminFindsUserByEmailAndWithdraws() {
        User user = userRepository.save(User.socialSignup("withdraw-mail@test.com", null, "메일요청회원"));
        socialAccountRepository.save(UserSocialAccount.builder()
                .userId(user.getUserId())
                .provider(SocialProvider.KAKAO)
                .providerId("kakao-mail-sub")
                .build());

        assertThat(adminUserService.search("메일요청회원"))
                .extracting(AdminUserResponse::userId)
                .containsExactly(user.getUserId());
        assertThat(adminUserService.search(" withdraw-mail@test.com "))
                .singleElement()
                .satisfies(found -> assertThat(found.socialProviders()).containsExactly(SocialProvider.KAKAO));

        authService.withdrawByAdmin(user.getUserId());

        assertThat(userRepository.findById(user.getUserId()))
                .get()
                .extracting(User::getStatus)
                .isEqualTo(UserStatus.WITHDRAWN);
        // 이메일·닉네임이 익명화돼 다시 찾을 수 없다.
        assertThat(adminUserService.search("withdraw-mail@test.com")).isEmpty();
        assertThat(socialAccountRepository.findByUserId(user.getUserId())).isEmpty();
    }

    private void saveSession(Long userId, String deviceId) {
        userSessionRepository.save(UserSession.builder()
                .userId(userId)
                .deviceId(deviceId)
                .sessionId("session-" + deviceId)
                .deviceName("테스트 기기")
                .platform(com.coremate.backend.auth.entity.DevicePlatform.ANDROID)
                .osVersion("15")
                .lastSeenAt(LocalDateTime.now())
                .build());
    }

    private static Notification poke(User receiver, User sender) {
        return new Notification(receiver, NotificationType.FRIEND_POKE, sender.getNickname() + "님이 콕 찔렀어요",
                "같이 공부하러 갈까요?", NotificationTargetType.FRIEND, sender.getUserId());
    }
}
