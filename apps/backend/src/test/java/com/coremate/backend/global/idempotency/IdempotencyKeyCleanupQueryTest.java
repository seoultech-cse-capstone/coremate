package com.coremate.backend.global.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * 보존 기간 정리(deleteByCreatedAtBefore)가 오래된 키만 지우고, 아직 유효한 키의 멱등성 보장은
 * 그대로 유지하는지 검증한다. created_at 은 엔티티 생성자가 now() 로 박으므로, 과거 시각 키는
 * 네이티브 INSERT 로 넣어 백데이팅한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("멱등성 키 보존 기간 정리")
class IdempotencyKeyCleanupQueryTest {

    private static final long OLD_USER_ID = 9_900_001L;
    private static final long FRESH_USER_ID = 9_900_002L;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("보존 기간이 지난 키만 삭제되고, 유효한 키는 남아 멱등성(중복 차단)이 유지된다")
    void 오래된_키만_삭제되고_유효키_멱등성_유지() {
        // 2일 전 키(보존 기간 1일 기준 만료) — 네이티브 INSERT 로 created_at 백데이팅
        entityManager.createNativeQuery(
                        "INSERT INTO idempotency_keys (user_id, idempotency_key, operation, created_at)"
                                + " VALUES (?, ?, ?, ?)")
                .setParameter(1, OLD_USER_ID)
                .setParameter(2, "expired-key")
                .setParameter(3, "TEST")
                .setParameter(4, LocalDateTime.now().minusDays(2))
                .executeUpdate();

        // 방금 생성된 유효 키
        idempotencyKeyRepository.saveAndFlush(
                new IdempotencyKey(FRESH_USER_ID, "fresh-key", "TEST"));

        long deleted = idempotencyKeyRepository.deleteByCreatedAtBefore(LocalDateTime.now().minusDays(1));
        entityManager.flush();
        entityManager.clear();

        assertThat(deleted).isEqualTo(1L);
        assertThat(idempotencyKeyRepository.count()).isEqualTo(1L);

        // 유효 키는 남아 있으므로 같은 (userId, key) 재삽입은 UNIQUE 위반 → 멱등성 보장 유지
        assertThatThrownBy(() -> idempotencyKeyRepository.saveAndFlush(
                new IdempotencyKey(FRESH_USER_ID, "fresh-key", "TEST")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
