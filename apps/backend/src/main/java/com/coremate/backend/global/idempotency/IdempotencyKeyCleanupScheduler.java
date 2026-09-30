package com.coremate.backend.global.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 매일 자정(KST)에 보존 기간이 지난 멱등성 키를 삭제해 idempotency_keys 테이블이 무한 적재되지 않게 한다.
// 삭제는 멱등적(같은 대상을 두 번 지워도 무해)이라 분산 락 없이 기존 자정 정리 스케줄러와 동일하게 둔다.
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyKeyCleanupScheduler {

    private final IdempotencyKeyCleanupService idempotencyKeyCleanupService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void cleanupExpiredKeys() {
        long deleted = idempotencyKeyCleanupService.deleteExpiredKeys();
        log.info("[Idempotency] 보존기간 지난 멱등성 키 {}건 삭제 완료", deleted);
    }
}
