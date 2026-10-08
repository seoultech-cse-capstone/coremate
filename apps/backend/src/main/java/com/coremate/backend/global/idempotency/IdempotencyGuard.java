package com.coremate.backend.global.idempotency;

import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

// 재화 차감·보상 지급처럼 이중 실행되면 안 되는 요청의 멱등성 가드.
// 반드시 해당 작업의 @Transactional 안에서 맨 먼저 호출한다:
//   - 같은 (userId, key) 가 이미 있으면 DUPLICATE_REQUEST 로 막는다(더블클릭·재시도·동시요청).
//   - 이후 비즈니스 로직이 실패하면 이 키 삽입도 같은 트랜잭션으로 롤백돼 같은 키로 재시도할 수 있다.
@Component
@RequiredArgsConstructor
public class IdempotencyGuard {

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    public void register(Long userId, String idempotencyKey, String operation) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new CoreMateException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }
        try {
            // saveAndFlush 로 즉시 INSERT 를 날려 UNIQUE 위반을 이 자리에서 잡는다.
            idempotencyKeyRepository.saveAndFlush(
                    new IdempotencyKey(userId, idempotencyKey.trim(), operation));
        } catch (DataIntegrityViolationException e) {
            throw new CoreMateException(ErrorCode.DUPLICATE_REQUEST);
        }
    }

    // 트랜잭션을 분할해 비즈니스 로직을 등록과 다른 트랜잭션에서 수행하는 경우, 실패 보상 단계에서
    // 이 메서드로 키를 지워 같은 키로 재시도할 수 있게 한다(등록·로직이 한 트랜잭션이면 롤백으로
    // 충분하므로 호출할 필요가 없다). 반드시 보상 트랜잭션 안에서 호출한다.
    public void unregister(Long userId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        idempotencyKeyRepository.deleteByUserIdAndIdempotencyKey(userId, idempotencyKey.trim());
    }
}
