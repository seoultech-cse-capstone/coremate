package com.coremate.backend.solve.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coremate.backend.content.entity.CategoryLv2;
import com.coremate.backend.content.entity.Subject;
import com.coremate.backend.reward.entity.RewardLog;
import com.coremate.backend.reward.entity.type.RewardSkipReason;
import com.coremate.backend.reward.entity.type.RewardSource;
import com.coremate.backend.reward.entity.type.RewardStatus;
import com.coremate.backend.reward.entity.type.RewardTargetType;
import com.coremate.backend.reward.entity.type.RewardType;
import com.coremate.backend.reward.service.RewardService;
import com.coremate.backend.solve.dto.type.SolveSessionType;
import com.coremate.backend.solve.entity.SolveSession;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * 모의고사 재응시 코인의 분기 조건을 검증한다.
 *
 * <p>핵심 규칙은 "최초 통과 정책이 ALREADY_REWARDED 로 걸린 회차만 재응시로 넘어간다"이다.
 * 정답률 미달이나 일일 한도 초과까지 넘기면 최초 통과 조건을 한 번도 만족하지 못한
 * 풀이가 재응시 보상으로 우회 지급된다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("모의고사 재응시 코인 판정")
class SolveRewardServiceRetryCoinTest {

    private static final Long USER_ID = 1L;
    private static final Long SESSION_ID = 100L;
    private static final Long CATEGORY_LV2_ID = 20L;
    private static final Long SUBJECT_ID = 3L;
    private static final BigDecimal PASSING_RATE = new BigDecimal("85.00");

    @Mock private RewardService rewardService;

    @InjectMocks private SolveRewardService solveRewardService;

    private SolveSession session;

    @BeforeEach
    void setUp() {
        session = mock(SolveSession.class);
        when(session.getUserId()).thenReturn(USER_ID);
        when(session.getSolveSessionId()).thenReturn(SESSION_ID);
        when(session.getSessionType()).thenReturn(SolveSessionType.CATEGORY_LV2_MOCK);

        CategoryLv2 categoryLv2 = mock(CategoryLv2.class);
        when(categoryLv2.getCategoryLv2Id()).thenReturn(CATEGORY_LV2_ID);
        when(session.getCategoryLv2()).thenReturn(categoryLv2);

        Subject subject = mock(Subject.class);
        when(subject.getSubjectId()).thenReturn(SUBJECT_ID);
        when(session.getSubject()).thenReturn(subject);
    }

    @Test
    @DisplayName("최초 보상을 이미 받은 범위는 재응시 정책으로 다시 판정된다")
    void 이미_받은_범위는_재응시로_넘어간다() {
        stubFirstPass(RewardType.CATEGORY_MOCK_FIRST_PASS_COIN, skipped(RewardSkipReason.ALREADY_REWARDED));
        stubRetry(RewardType.CATEGORY_MOCK_RETRY_COIN, granted(5));

        SolveRewardResult result = solveRewardService.grantForCompletedSession(session, PASSING_RATE);

        assertThat(result.earnedCoin()).isEqualTo(5);
        assertThat(result.coinSkipReason()).isNull();
    }

    @Test
    @DisplayName("재응시 보상은 '같은 범위 하루 1회' 판정을 위해 범위 타겟을 넘긴다")
    void 재응시는_범위_타겟을_넘긴다() {
        stubFirstPass(RewardType.CATEGORY_MOCK_FIRST_PASS_COIN, skipped(RewardSkipReason.ALREADY_REWARDED));
        stubRetry(RewardType.CATEGORY_MOCK_RETRY_COIN, granted(5));

        solveRewardService.grantForCompletedSession(session, PASSING_RATE);

        verify(rewardService).grantPolicyReward(
                eq(USER_ID),
                eq(RewardType.CATEGORY_MOCK_RETRY_COIN),
                eq(RewardSource.SOLVE_SESSION),
                eq(SESSION_ID),
                eq(RewardTargetType.CATEGORY_LV2),
                eq(CATEGORY_LV2_ID),
                eq(SESSION_ID),
                eq(PASSING_RATE)
        );
    }

    @Test
    @DisplayName("모의고사 XP 도 같은 범위 판정을 위해 범위 타겟을 넘긴다")
    void 모의고사_XP는_범위_타겟을_넘긴다() {
        stubFirstPass(RewardType.CATEGORY_MOCK_FIRST_PASS_COIN, granted(30));

        solveRewardService.grantForCompletedSession(session, PASSING_RATE);

        verify(rewardService).grantPolicyReward(
                eq(USER_ID),
                eq(RewardType.CATEGORY_MOCK_XP),
                eq(RewardSource.SOLVE_SESSION),
                eq(SESSION_ID),
                eq(RewardTargetType.CATEGORY_LV2),
                eq(CATEGORY_LV2_ID),
                eq(SESSION_ID)
        );
    }

    @Test
    @DisplayName("개념 문제집 XP 는 적용 대상이 아니라 범위 타겟 없이 요청된다")
    void 개념_문제집_XP는_타겟이_없다() {
        when(session.getSessionType()).thenReturn(SolveSessionType.CONCEPT_PRACTICE);

        solveRewardService.grantForCompletedSession(session, PASSING_RATE);

        verify(rewardService).grantPolicyReward(
                eq(USER_ID),
                eq(RewardType.CONCEPT_PRACTICE_XP),
                eq(RewardSource.SOLVE_SESSION),
                eq(SESSION_ID),
                isNull(),
                isNull(),
                eq(SESSION_ID)
        );
    }

    @Test
    @DisplayName("과목 모의고사는 과목을 범위 타겟으로 넘긴다")
    void 과목은_과목_타겟을_넘긴다() {
        when(session.getSessionType()).thenReturn(SolveSessionType.SUBJECT_MOCK);
        stubFirstPass(RewardType.SUBJECT_MOCK_FIRST_PASS_COIN, skipped(RewardSkipReason.ALREADY_REWARDED));
        stubRetry(RewardType.SUBJECT_MOCK_RETRY_COIN, granted(10));

        solveRewardService.grantForCompletedSession(session, PASSING_RATE);

        verify(rewardService).grantPolicyReward(
                eq(USER_ID),
                eq(RewardType.SUBJECT_MOCK_RETRY_COIN),
                eq(RewardSource.SOLVE_SESSION),
                eq(SESSION_ID),
                eq(RewardTargetType.SUBJECT),
                eq(SUBJECT_ID),
                eq(SESSION_ID),
                eq(PASSING_RATE)
        );
    }

    @Test
    @DisplayName("정답률 미달로 걸린 회차는 재응시 보상을 판정하지 않는다")
    void 정답률_미달은_재응시로_넘어가지_않는다() {
        stubFirstPass(RewardType.CATEGORY_MOCK_FIRST_PASS_COIN, skipped(RewardSkipReason.LOW_CORRECT_RATE));

        SolveRewardResult result = solveRewardService.grantForCompletedSession(session, new BigDecimal("50.00"));

        assertThat(result.earnedCoin()).isZero();
        assertThat(result.coinSkipReason()).isEqualTo(RewardSkipReason.LOW_CORRECT_RATE);
        verifyRetryNeverRequested(RewardType.CATEGORY_MOCK_RETRY_COIN);
    }

    @Test
    @DisplayName("일일 한도 초과로 걸린 회차는 재응시 보상을 판정하지 않는다")
    void 한도_초과는_재응시로_넘어가지_않는다() {
        stubFirstPass(RewardType.CATEGORY_MOCK_FIRST_PASS_COIN, skipped(RewardSkipReason.DAILY_LIMIT_EXCEEDED));

        SolveRewardResult result = solveRewardService.grantForCompletedSession(session, PASSING_RATE);

        assertThat(result.earnedCoin()).isZero();
        assertThat(result.coinSkipReason()).isEqualTo(RewardSkipReason.DAILY_LIMIT_EXCEEDED);
        verifyRetryNeverRequested(RewardType.CATEGORY_MOCK_RETRY_COIN);
    }

    @Test
    @DisplayName("최초 보상이 지급되면 재응시 보상은 판정하지 않는다")
    void 최초_보상을_받으면_재응시는_없다() {
        stubFirstPass(RewardType.CATEGORY_MOCK_FIRST_PASS_COIN, granted(30));

        SolveRewardResult result = solveRewardService.grantForCompletedSession(session, PASSING_RATE);

        assertThat(result.earnedCoin()).isEqualTo(30);
        verifyRetryNeverRequested(RewardType.CATEGORY_MOCK_RETRY_COIN);
    }

    @Test
    @DisplayName("과목 모의고사는 과목 재응시 타입을 쓴다")
    void 과목은_과목_재응시_타입을_쓴다() {
        when(session.getSessionType()).thenReturn(SolveSessionType.SUBJECT_MOCK);
        stubFirstPass(RewardType.SUBJECT_MOCK_FIRST_PASS_COIN, skipped(RewardSkipReason.ALREADY_REWARDED));
        stubRetry(RewardType.SUBJECT_MOCK_RETRY_COIN, granted(10));

        SolveRewardResult result = solveRewardService.grantForCompletedSession(session, PASSING_RATE);

        assertThat(result.earnedCoin()).isEqualTo(10);
        verifyRetryNeverRequested(RewardType.CATEGORY_MOCK_RETRY_COIN);
    }

    @Test
    @DisplayName("개념 문제집은 최초·재응시 어느 코인도 판정하지 않는다")
    void 개념_문제집은_코인_판정을_하지_않는다() {
        when(session.getSessionType()).thenReturn(SolveSessionType.CONCEPT_PRACTICE);

        SolveRewardResult result = solveRewardService.grantForCompletedSession(session, PASSING_RATE);

        assertThat(result.earnedCoin()).isZero();
        assertThat(result.coinSkipReason()).isNull();
        verifyRetryNeverRequested(RewardType.CATEGORY_MOCK_RETRY_COIN);
        verifyRetryNeverRequested(RewardType.CATEGORY_MOCK_FIRST_PASS_COIN);
    }

    private void stubFirstPass(RewardType rewardType, RewardLog log) {
        when(rewardService.grantPolicyReward(
                any(), eq(rewardType), any(), any(), any(), any(), any(), any()))
                .thenReturn(log);
    }

    private void stubRetry(RewardType rewardType, RewardLog log) {
        when(rewardService.grantPolicyReward(
                any(), eq(rewardType), any(), any(), any(), any(), any(), any()))
                .thenReturn(log);
    }

    private void verifyRetryNeverRequested(RewardType rewardType) {
        verify(rewardService, never()).grantPolicyReward(
                any(), eq(rewardType), any(), any(), any(), any(), any(), any());
    }

    private RewardLog skipped(RewardSkipReason reason) {
        RewardLog log = mock(RewardLog.class);
        when(log.getRewardStatus()).thenReturn(RewardStatus.SKIPPED);
        when(log.getSkipReason()).thenReturn(reason);
        return log;
    }

    private RewardLog granted(int coinAmount) {
        RewardLog log = mock(RewardLog.class);
        when(log.getRewardStatus()).thenReturn(RewardStatus.GRANTED);
        when(log.getCoinAmount()).thenReturn(coinAmount);
        return log;
    }
}
