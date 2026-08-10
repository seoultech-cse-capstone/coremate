package com.coremate.backend.dailyquiz.scheduler;

import com.coremate.backend.dailyquiz.service.DailyQuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 매일 자정(KST)에 전날 이전의 오늘의 퀴즈 선지 기록을 삭제해 DB에는 당일 데이터만 유지한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyQuizAnswerLogCleanupScheduler {

    private final DailyQuizService dailyQuizService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void cleanupOutdatedAnswerLogs() {
        long deleted = dailyQuizService.deleteOutdatedAnswerLogs();
        log.info("[DailyQuiz] 전날 이전 선지 기록 {}건 삭제 완료", deleted);
    }
}
