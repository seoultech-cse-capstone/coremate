package com.coremate.backend.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 정기 알림 발송 시각(FR-NOTI-06, FR-NOTI-07).
//
// 발송 시각은 요구사항에 확정돼 있어 설정으로 빼지 않고 상수로 둔다. 바꾸려면 요구사항부터
// 바뀌어야 하는 값이라, 환경변수로 열어 두면 서버마다 다른 시각에 알림이 나갈 수 있다.
//
// ⚠️ 인스턴스가 둘 이상이면 각자 스케줄러를 돌려 같은 시각에 동시에 대상자를 뽑는다.
// 발송 로그 기준 중복 차단은 "이미 보낸 뒤"에만 효과가 있어, 동시 실행은 막지 못한다.
// 다중 인스턴스로 배포할 때 분산 락이 필요하다(NFR-DATA-09) — 지금은 단일 인스턴스 전제.
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final ScheduledNotificationService scheduledNotificationService;

    // 매일 08:00 — 출근·등교 시간대. 오늘의 퀴즈는 5문제라 이동 중에도 소화할 수 있다.
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Seoul")
    public void sendDailyQuizOpen() {
        scheduledNotificationService.sendDailyQuizOpen();
    }

    // 매일 21:00 — 자정 마감 3시간 전. 그날 출석하지 않은 회원에게만 간다.
    @Scheduled(cron = "0 0 21 * * *", zone = "Asia/Seoul")
    public void sendAttendanceReminder() {
        scheduledNotificationService.sendAttendanceReminder();
    }

    // 매주 일요일 19:00 — 주 1회. 매일 보내면 알림 자체를 꺼 버리게 된다.
    @Scheduled(cron = "0 0 19 * * SUN", zone = "Asia/Seoul")
    public void sendLearningReminder() {
        scheduledNotificationService.sendLearningReminder();
    }
}
