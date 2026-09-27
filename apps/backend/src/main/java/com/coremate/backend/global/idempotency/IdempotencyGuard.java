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
}
