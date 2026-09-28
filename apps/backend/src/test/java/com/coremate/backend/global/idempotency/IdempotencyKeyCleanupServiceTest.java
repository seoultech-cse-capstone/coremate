package com.coremate.backend.global.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("멱등성 키 정리 서비스")
class IdempotencyKeyCleanupServiceTest {

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Test
    @DisplayName("보존 기간(일)만큼 과거를 기준으로 오래된 키를 삭제하고 삭제 건수를 돌려준다")
    void 보존기간_기준_삭제() {
        int retentionDays = 1;
        IdempotencyKeyCleanupService service =
                new IdempotencyKeyCleanupService(idempotencyKeyRepository, retentionDays);
        given(idempotencyKeyRepository.deleteByCreatedAtBefore(org.mockito.ArgumentMatchers.any()))
                .willReturn(3L);

        LocalDateTime before = LocalDateTime.now().minusDays(retentionDays);
        long deleted = service.deleteExpiredKeys();
        LocalDateTime after = LocalDateTime.now().minusDays(retentionDays);

        assertThat(deleted).isEqualTo(3L);

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        org.mockito.Mockito.verify(idempotencyKeyRepository).deleteByCreatedAtBefore(captor.capture());
        // 임계값은 "지금 - 보존기간"이어야 한다(호출 전후로 잡은 범위 안).
        assertThat(captor.getValue()).isBetween(before, after);
    }
}
