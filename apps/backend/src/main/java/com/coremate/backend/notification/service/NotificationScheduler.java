package com.coremate.backend.notification.service;

import com.coremate.backend.global.lock.SchedulerLock;
import com.coremate.backend.notification.entity.type.NotificationType;
import java.time.Duration;
import java.time.LocalDate;
import java.util.function.IntSupplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 정기 알림 발송 시각(FR-NOTI-06, FR-NOTI-07).
//
// 발송 시각은 요구사항에 확정돼 있어 설정으로 빼지 않고 상수로 둔다. 바꾸려면 요구사항부터
// 바뀌어야 하는 값이라, 환경변수로 열어 두면 서버마다 다른 시각에 알림이 나갈 수 있다.
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    // 하루가 지나면 자동으로 풀리게 두어, 다음 날 같은 작업이 정상적으로 다시 돈다.
    private static final Duration LOCK_TTL = Duration.ofHours(23);

    private final ScheduledNotificationService scheduledNotificationService;
    private final SchedulerLock schedulerLock;

    // 매일 08:00 — 출근·등교 시간대. 오늘의 퀴즈는 5문제라 이동 중에도 소화할 수 있다.
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Seoul")
    public void sendDailyQuizOpen() {
        runOnce(NotificationType.DAILY_QUIZ_OPEN, scheduledNotificationService::sendDailyQuizOpen);
    }

    // 매일 21:00 — 자정 마감 3시간 전. 그날 출석하지 않은 회원에게만 간다.
    @Scheduled(cron = "0 0 21 * * *", zone = "Asia/Seoul")
    public void sendAttendanceReminder() {
        runOnce(NotificationType.ATTENDANCE_REMINDER, scheduledNotificationService::sendAttendanceReminder);
    }

    // 매주 일요일 19:00 — 주 1회. 매일 보내면 알림 자체를 꺼 버리게 된다.
    @Scheduled(cron = "0 0 19 * * SUN", zone = "Asia/Seoul")
    public void sendLearningReminder() {
        runOnce(NotificationType.LEARNING_REMINDER, scheduledNotificationService::sendLearningReminder);
    }

    // 인스턴스가 여럿이면 각자 같은 시각에 스케줄러를 돌린다. 발송 로그 기준 중복 차단은
    // "이미 보낸 뒤"에만 효과가 있어 동시 실행은 막지 못하므로, 잠금을 먼저 잡은 인스턴스만
    // 실제로 발송한다(NFR-DATA-09).
    //
    // 잠금 키에 날짜를 넣어 다음 날 같은 작업이 정상적으로 다시 돌게 한다.
    private void runOnce(NotificationType type, IntSupplier job) {
        String lockKey = "notification:" + type.name() + ":" + LocalDate.now();
        if (!schedulerLock.tryAcquire(lockKey, LOCK_TTL)) {
            log.info("다른 인스턴스가 이미 실행 중이라 건너뛴다. type={}", type);
            return;
        }
        job.getAsInt();
    }
}
