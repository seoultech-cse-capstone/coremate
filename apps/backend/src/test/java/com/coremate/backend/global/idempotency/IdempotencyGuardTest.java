package com.coremate.backend.global.idempotency;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("멱등성 가드")
class IdempotencyGuardTest {

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @InjectMocks
    private IdempotencyGuard idempotencyGuard;

    @Test
    @DisplayName("처음 등록은 정상 통과한다")
    void 처음_등록_정상() {
        assertThatCode(() -> idempotencyGuard.register(1L, "key-1", "TEST"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("UNIQUE 위반(같은 키 재등록)이면 DUPLICATE_REQUEST 로 변환한다")
    void 중복_키면_중복요청() {
        given(idempotencyKeyRepository.saveAndFlush(org.mockito.ArgumentMatchers.any()))
                .willThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> idempotencyGuard.register(1L, "key-1", "TEST"))
                .isInstanceOf(CoreMateException.class)
                .extracting(e -> ((CoreMateException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_REQUEST);
    }

    @Test
    @DisplayName("키가 비어 있으면 저장 시도 없이 IDEMPOTENCY_KEY_REQUIRED 로 막는다")
    void 빈_키면_키필요() {
        assertThatThrownBy(() -> idempotencyGuard.register(1L, "  ", "TEST"))
                .isInstanceOf(CoreMateException.class)
                .extracting(e -> ((CoreMateException) e).getErrorCode())
                .isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);

        assertThatThrownBy(() -> idempotencyGuard.register(1L, null, "TEST"))
                .isInstanceOf(CoreMateException.class)
                .extracting(e -> ((CoreMateException) e).getErrorCode())
                .isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);

        verifyNoInteractions(idempotencyKeyRepository);
    }
}
