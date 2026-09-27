package com.coremate.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.coremate.backend.attendance.entity.Attendance;
import com.coremate.backend.attendance.repository.AttendanceRepository;
import com.coremate.backend.notification.dto.response.NotificationResponse;
import com.coremate.backend.notification.entity.type.NotificationType;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserRole;
import com.coremate.backend.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

// 정기 알림의 핵심은 "누구에게 보낼지 고르는 규칙"이다. 시각(cron)은 스프링이 담당하므로
// 여기서는 대상자 산정과 당일 중복 차단만 검증한다.
@SpringBootTest
@Transactional
class ScheduledNotificationServiceTest {

    @Autowired
    private ScheduledNotificationService scheduledNotificationService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void attendanceReminderSkipsUsersWhoAlreadyAttended() {
        User attended = createUser("sch1@example.com", "sch1");
        User notYet = createUser("sch2@example.com", "sch2");
        attendanceRepository.save(new Attendance(attended, LocalDate.now(), null));

        scheduledNotificationService.sendAttendanceReminder();

        assertThat(typesOf(attended)).doesNotContain(NotificationType.ATTENDANCE_REMINDER);
        assertThat(typesOf(notYet)).contains(NotificationType.ATTENDANCE_REMINDER);
    }

    @Test
    void dailyQuizNotificationIsSentOnlyOncePerDay() {
        User user = createUser("sch3@example.com", "sch3");

        scheduledNotificationService.sendDailyQuizOpen();
        scheduledNotificationService.sendDailyQuizOpen();

        // 스케줄러가 재시도되거나 배포 직후 중복 실행돼도 사용자에게는 한 번만 가야 한다.
        assertThat(typesOf(user).stream().filter(NotificationType.DAILY_QUIZ_OPEN::equals).count())
                .isEqualTo(1);
    }

    @Test
    void learningReminderTargetsUsersWithoutRecentSolveSession() {
        User idle = createUser("sch4@example.com", "sch4");

        scheduledNotificationService.sendLearningReminder();

        // 풀이 기록이 전혀 없는 사용자는 3일 이상 미학습으로 잡힌다.
        assertThat(typesOf(idle)).contains(NotificationType.LEARNING_REMINDER);
    }

    private List<NotificationType> typesOf(User user) {
        return notificationService.getNotifications(user.getUserId()).stream()
                .map(NotificationResponse::notificationType)
                .toList();
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
