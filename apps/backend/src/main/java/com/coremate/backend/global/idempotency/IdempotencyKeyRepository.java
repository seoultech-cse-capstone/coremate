package com.coremate.backend.global.idempotency;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    // 보존 기간이 지난(created_at < threshold) 키를 일괄 삭제하고 삭제된 건수를 돌려준다.
    long deleteByCreatedAtBefore(LocalDateTime threshold);

    // 비즈니스 로직이 별도 트랜잭션에서 실패했을 때, 같은 키로 재시도할 수 있도록 지운다.
    long deleteByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);
}
