package com.coremate.backend.solve.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.solve.dto.request.CompleteSolveSessionRequest;
import com.coremate.backend.solve.dto.type.SolveSessionStatus;
import com.coremate.backend.solve.dto.type.SolveSessionType;
import com.coremate.backend.solve.entity.SolveSession;
import com.coremate.backend.solve.repository.SolveSessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 모의고사 회차 중단 처리 통합 테스트(H2).
 *
 * <p>제한 시간이 지난 회차는 요청을 거부당하면서도 중단 상태만은 남아야 한다(REQUIRES_NEW).
 * 테스트에 @Transactional 을 걸면 그 경계가 가려지므로 서비스가 트랜잭션을 직접 관리하게 두고,
 * 만든 회차는 매 테스트 뒤에 지운다.
 */
@SpringBootTest
class SolveSessionAbandonIntegrationTest {

    private static final Long USER_ID = 90_001L;
    private static final Set<SolveSessionType> MOCK_TYPES = Set.of(
            SolveSessionType.CATEGORY_LV1_MOCK,
            SolveSessionType.CATEGORY_LV2_MOCK,
            SolveSessionType.SUBJECT_MOCK);

    @Autowired
    private SolveSessionService solveSessionService;

    @Autowired
    private SolveSessionRepository solveSessionRepository;

    @AfterEach
    void cleanUp() {
        solveSessionRepository.deleteAll(
                solveSessionRepository.findAll().stream()
                        .filter(s -> USER_ID.equals(s.getUserId()))
                        .toList());
    }

    @Test
    void 제한시간이_지난_챕터_모의고사는_완료를_거부하고_중단_상태로_남긴다() {
        SolveSession session = saveSession(SolveSessionType.CATEGORY_LV1_MOCK, 41);

        assertThatThrownBy(() -> complete(session))
                .isInstanceOf(CoreMateException.class)
                .extracting(e -> ((CoreMateException) e).getErrorCode())
                .isEqualTo(ErrorCode.SOLVE_SESSION_EXPIRED);

        // 완료 요청은 롤백되지만 중단 처리는 별도 트랜잭션이라 살아남아야 한다.
        assertThat(reload(session).getStatus()).isEqualTo(SolveSessionStatus.ABANDONED);
    }

    @Test
    void 챕터_제한시간_40분을_과목_모의고사에_적용하지_않는다() {
        // 41분 경과. 챕터였다면 만료지만 과목은 60분이라 아직 유효하다.
        SolveSession session = saveSession(SolveSessionType.SUBJECT_MOCK, 41);

        assertThatThrownBy(() -> complete(session))
                .isInstanceOf(CoreMateException.class)
                .extracting(e -> ((CoreMateException) e).getErrorCode())
                .isEqualTo(ErrorCode.SOLVE_NOT_ALL_ANSWERED);

        assertThat(reload(session).getStatus()).isEqualTo(SolveSessionStatus.IN_PROGRESS);
    }

    @Test
    void 진행_중_회차_조회는_모의고사만_대상으로_한다() {
        saveSession(SolveSessionType.CATEGORY_LV1_MOCK, 0);
        saveSession(SolveSessionType.SUBJECT_MOCK, 0);
        saveSession(SolveSessionType.CONCEPT_PRACTICE, 0);

        List<SolveSession> inProgressMocks = solveSessionRepository
                .findByUserIdAndStatusAndSessionTypeIn(USER_ID, SolveSessionStatus.IN_PROGRESS, MOCK_TYPES);

        assertThat(inProgressMocks)
                .extracting(SolveSession::getSessionType)
                .containsExactlyInAnyOrder(
                        SolveSessionType.CATEGORY_LV1_MOCK, SolveSessionType.SUBJECT_MOCK);
    }

    private SolveSession saveSession(SolveSessionType sessionType, int startedMinutesAgo) {
        SolveSession session = SolveSession.builder()
                .userId(USER_ID)
                .sessionType(sessionType)
                .title("중단 처리 테스트 회차")
                .totalQuestionCount(20)
                .questionIdsJson("[]")
                .build();
        ReflectionTestUtils.setField(
                session, "startedAt", LocalDateTime.now().minusMinutes(startedMinutesAgo));
        return solveSessionRepository.save(session);
    }

    private void complete(SolveSession session) {
        solveSessionService.completeSolveSession(
                session.getSolveSessionId(), USER_ID, new CompleteSolveSessionRequest(0));
    }

    private SolveSession reload(SolveSession session) {
        return solveSessionRepository.findById(session.getSolveSessionId()).orElseThrow();
    }
}
