package com.coremate.backend.global.idempotency;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 보존 기간이 지난 멱등성 키를 정리한다.
//
// 멱등성 판정에 필요한 기간은 클라이언트 재시도 윈도우(더블클릭·네트워크 재시도·오프라인 재전송)
// 수준이라 짧다. 정상 처리된 키는 삭제 경로가 없어 무한 적재되므로, created_at 기준으로 오래된
// 키를 주기적으로 지운다. 보존 기간은 idempotency.retention-days 로 조정한다(기본 1일).
@Slf4j
@Service
public class IdempotencyKeyCleanupService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final int retentionDays;

    public IdempotencyKeyCleanupService(
            IdempotencyKeyRepository idempotencyKeyRepository,
            @Value("${idempotency.retention-days:1}") int retentionDays) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.retentionDays = retentionDays;
    }

    // 보존 기간이 지난 키를 삭제하고 삭제 건수를 돌려준다.
    // 재시도 윈도우가 지난 키만 지우므로, 아직 유효한 키의 멱등성 보장은 그대로 유지된다.
    @Transactional
    public long deleteExpiredKeys() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
        return idempotencyKeyRepository.deleteByCreatedAtBefore(threshold);
    }
}
