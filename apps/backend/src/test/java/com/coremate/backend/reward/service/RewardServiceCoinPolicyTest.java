package com.coremate.backend.reward.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coremate.backend.reward.entity.DailyRewardLimit;
import com.coremate.backend.reward.entity.RewardLog;
import com.coremate.backend.reward.entity.RewardPolicy;
import com.coremate.backend.reward.entity.type.RewardScope;
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
import java.math.BigDecimal;
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
 * 코인 보상의 최소 정답률 판정과, 앞 단계에서 걸러진 요청이 일일 한도를 소모하지 않는지 검증한다.
 *
 * <p>판정 순서: 활성 정책 -> 범위별 최초 보상 -> 정답률 -> 일일 한도 소모.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("코인 보상 정답률 판정")
class RewardServiceCoinPolicyTest {

    private static final Long USER_ID = 1L;
    private static final Long CATEGORY_ID = 10L;

    @Mock private UserRepository userRepository;
    @Mock private RewardUserProgressRepository rewardUserProgressRepository;
    @Mock private UserCurrencyRepository userCurrencyRepository;
    @Mock private CurrencyLogRepository currencyLogRepository;
    @Mock private XpLogRepository xpLogRepository;
    @Mock private RewardLogRepository rewardLogRepository;
    @Mock private DailyRewardLimitRepository dailyRewardLimitRepository;
    @Mock private RewardPolicyRepository rewardPolicyRepository;

    @InjectMocks private RewardService rewardService;

    @BeforeEach
    void setUp() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(rewardLogRepository.save(any(RewardLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // 챕터 모의고사 코인 정책: 30코인, 정답률 70% 이상, 범위별 최초 1회, 하루 7회
        RewardPolicy policy = mock(RewardPolicy.class);
        when(policy.getRewardPolicyId()).thenReturn(1L);
        when(policy.getRewardType()).thenReturn(RewardType.CATEGORY_MOCK_FIRST_PASS_COIN);
        when(policy.getCoinAmount()).thenReturn(30);
        when(policy.getXpAmount()).thenReturn(0);
        when(policy.getMinCorrectRate()).thenReturn(new BigDecimal("70.00"));
        when(policy.isFirstRewardOnly()).thenReturn(true);
        when(policy.getRewardScope()).thenReturn(RewardScope.PER_CATEGORY_LV2);
        when(policy.hasDailyLimit()).thenReturn(true);
        when(policy.getDailyLimit()).thenReturn(7);

        when(rewardPolicyRepository.findByRewardTypeAndActiveTrue(RewardType.CATEGORY_MOCK_FIRST_PASS_COIN))
                .thenReturn(Optional.of(policy));
    }

    @Test
    @DisplayName("정답률이 기준 미만이면 LOW_CORRECT_RATE 로 미지급된다")
    void 정답률_미달이면_미지급() {
        RewardLog log = grant(new BigDecimal("69.99"));

        assertThat(log.getRewardStatus()).isEqualTo(RewardStatus.SKIPPED);
        assertThat(log.getSkipReason()).isEqualTo(RewardSkipReason.LOW_CORRECT_RATE);
    }

    @Test
    @DisplayName("정답률 미달 요청은 일일 한도를 소모하지 않는다")
    void 정답률_미달은_한도를_소모하지_않는다() {
        grant(new BigDecimal("50.00"));

        verify(dailyRewardLimitRepository, never())
                .findByUserIdAndRewardDateAndRewardTypeForUpdate(anyLong(), any(), any());
    }

    @Test
    @DisplayName("정답률이 기준과 같으면 정답률 단계를 통과해 일일 한도까지 간다 (이상 조건)")
    void 기준과_같으면_통과() {
        // 한도가 소진된 상태로 두면, 정답률 단계를 지나 한도 단계까지 갔다는 것이
        // skip_reason 으로 드러난다.
        DailyRewardLimit exhausted = mock(DailyRewardLimit.class);
        when(exhausted.canConsume()).thenReturn(false);
        when(dailyRewardLimitRepository.findByUserIdAndRewardDateAndRewardTypeForUpdate(
                anyLong(), any(), any())).thenReturn(Optional.of(exhausted));

        RewardLog log = grant(new BigDecimal("70.00"));

        assertThat(log.getSkipReason()).isEqualTo(RewardSkipReason.DAILY_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("정책에 기준이 있는데 정답률이 전달되지 않으면 지급하지 않는다")
    void 정답률이_없으면_미지급() {
        RewardLog log = grant(null);

        assertThat(log.getRewardStatus()).isEqualTo(RewardStatus.SKIPPED);
        assertThat(log.getSkipReason()).isEqualTo(RewardSkipReason.LOW_CORRECT_RATE);
    }

    @Test
    @DisplayName("이미 보상받은 범위는 정답률보다 먼저 걸러지고 한도도 소모하지 않는다")
    void 이미_보상받은_범위는_한도를_소모하지_않는다() {
        when(rewardLogRepository.existsByUserIdAndRewardTypeAndTargetTypeAndTargetIdAndRewardStatus(
                USER_ID, RewardType.CATEGORY_MOCK_FIRST_PASS_COIN,
                RewardTargetType.CATEGORY_LV2, CATEGORY_ID, RewardStatus.GRANTED))
                .thenReturn(true);

        RewardLog log = grant(new BigDecimal("100.00"));

        assertThat(log.getSkipReason()).isEqualTo(RewardSkipReason.ALREADY_REWARDED);
        verify(dailyRewardLimitRepository, never())
                .findByUserIdAndRewardDateAndRewardTypeForUpdate(anyLong(), any(), any());
    }

    private RewardLog grant(BigDecimal correctRate) {
        return rewardService.grantPolicyReward(
                USER_ID,
                RewardType.CATEGORY_MOCK_FIRST_PASS_COIN,
                RewardSource.SOLVE_SESSION,
                100L,
                RewardTargetType.CATEGORY_LV2,
                CATEGORY_ID,
                100L,
                correctRate
        );
    }
}
