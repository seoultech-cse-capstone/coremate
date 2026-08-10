package com.coremate.backend.notification.service;

import com.coremate.backend.notification.entity.type.NotificationType;
import com.coremate.backend.notification.repository.NotificationSendLogRepository;
import com.coremate.backend.notification.repository.NotificationTargetRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// 정기 알림 발송. 스케줄러가 시각을 정하고 여기서는 대상자를 뽑아 보낸다.
//
// 이 클래스에는 트랜잭션을 걸지 않는다. 대상자 전체를 한 트랜잭션으로 묶으면 한 사람에게서
// 난 DB 오류가 나머지 발송까지 되돌린다. 발송 한 건이 각자 트랜잭션을 열게 두면 실패가
// 그 사람에게서 끝난다.
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledNotificationService {

    // 최근 며칠 학습이 없으면 리마인드 대상으로 볼지(FR-NOTI-07).
    private static final int LEARNING_REMINDER_IDLE_DAYS = 3;

    private final NotificationTargetRepository notificationTargetRepository;
    private final NotificationSendLogRepository notificationSendLogRepository;
    private final NotificationSender notificationSender;

    public int sendDailyQuizOpen() {
        return sendToTargets(
                NotificationType.DAILY_QUIZ_OPEN,
                notificationTargetRepository.findActiveUserIds(),
                NotificationMessages::dailyQuizOpen);
    }

    public int sendAttendanceReminder() {
        return sendToTargets(
                NotificationType.ATTENDANCE_REMINDER,
                notificationTargetRepository.findActiveUserIdsWithoutAttendanceOn(LocalDate.now()),
                NotificationMessages::attendanceReminder);
    }

    public int sendLearningReminder() {
        LocalDateTime since = LocalDate.now().atStartOfDay().minusDays(LEARNING_REMINDER_IDLE_DAYS);
        return sendToTargets(
                NotificationType.LEARNING_REMINDER,
                notificationTargetRepository.findActiveUserIdsWithoutSolveSessionSince(since),
                NotificationMessages::learningReminder);
    }

    private int sendToTargets(
            NotificationType type, List<Long> targetUserIds, Function<Long, NotificationCommand> messageFactory) {
        Set<Long> alreadySent = new HashSet<>(findUserIdsSentToday(type));

        int sent = 0;
        for (Long userId : targetUserIds) {
            if (alreadySent.contains(userId)) {
                continue;
            }
            notificationSender.send(messageFactory.apply(userId));
            sent++;
        }

        log.info("정기 알림 발송 완료. type={} 대상={} 발송={} 당일중복제외={}",
                type, targetUserIds.size(), sent, targetUserIds.size() - sent);
        return sent;
    }

    // 같은 유형을 같은 날 두 번 보내지 않는다(NFR-DATA-09). 스케줄러가 재시도되거나
    // 배포 직후 중복 실행되어도 사용자에게 두 번 가지 않게 한다.
    private List<Long> findUserIdsSentToday(NotificationType type) {
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        return notificationSendLogRepository.findUserIdsSentBetween(type, dayStart, dayStart.plusDays(1));
    }
}
