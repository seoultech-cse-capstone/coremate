package com.coremate.backend.reward.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coremate.backend.reward.entity.DailyRewardLimit;
import com.coremate.backend.reward.entity.RewardLog;
import com.coremate.backend.reward.entity.RewardPolicy;
import com.coremate.backend.reward.entity.type.RewardSkipReason;
import com.coremate.backend.reward.entity.type.RewardSource;
import com.coremate.backend.reward.entity.type.RewardStatus;
import com.coremate.backend.reward.entity.type.RewardTargetType;
import com.coremate.backend.reward.entity.type.RewardType;
import com.coremate.backend.reward.repository.CurrencyLogRepository;
import com.coremate.backend.reward.repository.DailyRewardLimitRepository;
import com.coremate.backend.reward.repository.RewardLogRepository;
import com.coremate.backend.reward.repository.RewardPolicyRepository;
import com.coremate.backend.reward.repository.RewardUserProgressRepository;
import com.coremate.backend.reward.repository.UserCurrencyRepository;
import com.coremate.backend.reward.repository.XpLogRepository;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
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
 * {@code scope_daily_once} 정책의 "같은 범위는 하루 1회" 판정을 검증한다 (2026-08-11).
 *
 * <p>판정 순서: 활성 정책 -> 범위별 평생 1회 -> 범위별 당일 1회 -> 정답률 -> 일일 한도 소모.
 * 당일 중복으로 걸린 회차가 일일 한도를 소모하면, 같은 챕터를 다시 푸는 것만으로
 * 그날 몫이 깎이므로 반드시 한도 앞에서 걸러져야 한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("같은 범위 하루 1회 판정")
class RewardServiceScopeDailyOnceTest {

    private static final Long USER_ID = 1L;
    private static final Long CATEGORY_ID = 20L;
    private static final RewardType XP_TYPE = RewardType.CATEGORY_MOCK_XP;

    @Mock private UserRepository userRepository;
    @Mock private RewardUserProgressRepository rewardUserProgressRepository;
    @Mock private UserCurrencyRepository userCurrencyRepository;
    @Mock private CurrencyLogRepository currencyLogRepository;
    @Mock private XpLogRepository xpLogRepository;
    @Mock private RewardLogRepository rewardLogRepository;
    @Mock private DailyRewardLimitRepository dailyRewardLimitRepository;
    @Mock private RewardPolicyRepository rewardPolicyRepository;

    @InjectMocks private RewardService rewardService;

    private RewardPolicy policy;

    @BeforeEach
    void setUp() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(rewardLogRepository.save(any(RewardLog.class))).thenAnswer(inv -> inv.getArgument(0));
        // 지급까지 가는 경로를 위한 최소 스텁. 한도 행은 새로 만들어져 canConsume() 이 true 다.
        when(dailyRewardLimitRepository.save(any(DailyRewardLimit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rewardUserProgressRepository.updateLearningProgress(anyLong(), anyInt(), anyInt())).thenReturn(1);

        // 챕터 모의고사 XP: 30 XP, 정답률 무관, 평생 1회 아님, 같은 범위 하루 1회, 하루 7개
        policy = mock(RewardPolicy.class);
        when(policy.getRewardPolicyId()).thenReturn(1L);
        when(policy.getRewardType()).thenReturn(XP_TYPE);
        when(policy.getCoinAmount()).thenReturn(0);
        when(policy.getXpAmount()).thenReturn(30);
        when(policy.getMinCorrectRate()).thenReturn(null);
        when(policy.isFirstRewardOnly()).thenReturn(false);
        when(policy.isScopeDailyOnce()).thenReturn(true);
        when(policy.hasDailyLimit()).thenReturn(true);
        when(policy.getDailyLimit()).thenReturn(7);

        when(rewardPolicyRepository.findByRewardTypeAndActiveTrue(XP_TYPE)).thenReturn(Optional.of(policy));
    }

    @Test
    @DisplayName("같은 범위로 오늘 이미 받았으면 ALREADY_REWARDED_TODAY 로 미지급된다")
    void 당일_같은_범위는_미지급() {
        stubRewardedToday(true);

        RewardLog log = grant();

        assertThat(log.getRewardStatus()).isEqualTo(RewardStatus.SKIPPED);
        assertThat(log.getSkipReason()).isEqualTo(RewardSkipReason.ALREADY_REWARDED_TODAY);
    }

    @Test
    @DisplayName("당일 중복으로 걸린 요청은 일일 한도를 소모하지 않는다")
    void 당일_중복은_한도를_소모하지_않는다() {
        stubRewardedToday(true);

        grant();

        verify(dailyRewardLimitRepository, never())
                .findByUserIdAndRewardDateAndRewardTypeForUpdate(anyLong(), any(), any());
    }

    @Test
    @DisplayName("오늘 기록이 없으면 통과해 일일 한도까지 간다")
    void 오늘_기록이_없으면_통과() {
        stubRewardedToday(false);

        RewardLog log = grant();

        assertThat(log.getRewardStatus()).isEqualTo(RewardStatus.GRANTED);
    }

    @Test
    @DisplayName("판정은 오늘 0시 이후의 GRANTED 로그만 본다")
    void 오늘_0시_이후만_본다() {
        stubRewardedToday(false);

        grant();

        verify(rewardLogRepository)
                .existsByUserIdAndRewardTypeAndTargetTypeAndTargetIdAndRewardStatusAndCreatedAtGreaterThanEqual(
                        eq(USER_ID), eq(XP_TYPE), eq(RewardTargetType.CATEGORY_LV2), eq(CATEGORY_ID),
                        eq(RewardStatus.GRANTED), eq(LocalDate.now().atStartOfDay()));
    }

    @Test
    @DisplayName("플래그가 꺼진 정책은 오늘 기록이 있어도 지급된다")
    void 플래그가_꺼지면_반복_지급된다() {
        when(policy.isScopeDailyOnce()).thenReturn(false);
        stubRewardedToday(true);

        RewardLog log = grant();

        assertThat(log.getRewardStatus()).isEqualTo(RewardStatus.GRANTED);
    }

    @Test
    @DisplayName("범위 타겟이 없으면 판정할 수 없으므로 조회하지 않고 통과시킨다")
    void 타겟이_없으면_판정하지_않는다() {
        RewardLog log = rewardService.grantPolicyReward(
                USER_ID, XP_TYPE, RewardSource.SOLVE_SESSION, 100L, null, null, 100L);

        assertThat(log.getRewardStatus()).isEqualTo(RewardStatus.GRANTED);
        verify(rewardLogRepository, never())
                .existsByUserIdAndRewardTypeAndTargetTypeAndTargetIdAndRewardStatusAndCreatedAtGreaterThanEqual(
                        any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("평생 1회 판정이 당일 판정보다 먼저다")
    void 평생_1회가_먼저_걸린다() {
        when(policy.isFirstRewardOnly()).thenReturn(true);
        when(rewardLogRepository.existsByUserIdAndRewardTypeAndTargetTypeAndTargetIdAndRewardStatus(
                USER_ID, XP_TYPE, RewardTargetType.CATEGORY_LV2, CATEGORY_ID, RewardStatus.GRANTED))
                .thenReturn(true);
        stubRewardedToday(true);

        RewardLog log = grant();

        assertThat(log.getSkipReason()).isEqualTo(RewardSkipReason.ALREADY_REWARDED);
    }

    private void stubRewardedToday(boolean rewarded) {
        when(rewardLogRepository
                .existsByUserIdAndRewardTypeAndTargetTypeAndTargetIdAndRewardStatusAndCreatedAtGreaterThanEqual(
                        eq(USER_ID), eq(XP_TYPE), eq(RewardTargetType.CATEGORY_LV2), eq(CATEGORY_ID),
                        eq(RewardStatus.GRANTED), any()))
                .thenReturn(rewarded);
    }

    private RewardLog grant() {
        return rewardService.grantPolicyReward(
                USER_ID,
                XP_TYPE,
                RewardSource.SOLVE_SESSION,
                100L,
                RewardTargetType.CATEGORY_LV2,
                CATEGORY_ID,
                100L
        );
    }
}
