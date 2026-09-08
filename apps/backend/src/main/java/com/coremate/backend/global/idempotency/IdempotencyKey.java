package com.coremate.backend.global.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 상태 변경 요청의 멱등성 보장용 키. (user_id, idempotency_key) 를 UNIQUE 로 두어
// 같은 사용자가 같은 키로 두 번 요청하면 두 번째가 제약 위반으로 막힌다.
@Entity
@Table(
        name = "idempotency_keys",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_idempotency_user_key",
                columnNames = {"user_id", "idempotency_key"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    // 어떤 작업의 키인지(디버깅·조회용). 유니크에는 포함하지 않는다.
    @Column(name = "operation", nullable = false, length = 50)
    private String operation;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public IdempotencyKey(Long userId, String idempotencyKey, String operation) {
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.operation = operation;
        this.createdAt = LocalDateTime.now();
    }
}
