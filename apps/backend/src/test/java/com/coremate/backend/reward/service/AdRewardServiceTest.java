package com.coremate.backend.reward.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.reward.entity.AdRewardLog;
import com.coremate.backend.reward.entity.CurrencyLog;
import com.coremate.backend.reward.entity.RewardLog;
import com.coremate.backend.reward.entity.type.AdRewardStatus;
import com.coremate.backend.reward.entity.type.RewardSkipReason;
import com.coremate.backend.reward.entity.type.RewardSource;
import com.coremate.backend.reward.entity.type.RewardType;
import com.coremate.backend.reward.repository.AdRewardLogRepository;
import com.coremate.backend.reward.repository.CurrencyLogRepository;
import com.coremate.backend.user.repository.UserRepository;
import com.google.crypto.tink.apps.rewardedads.RewardedAdsVerifier;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 광고 시청 완료 콜백(AdMob 서버 측 확인)의 서명 검증과 지급 판정을 검증한다.
 *
 * <p>구글 대신 테스트에서 만든 키로 서명한다. 검증은 실제 라이브러리(RewardedAdsVerifier)가 한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("광고 시청 보상 콜백")
class AdRewardServiceTest {

    private static final long KEY_ID = 1234L;
    private static final Long USER_ID = 42L;
    private static final Long AD_REWARD_ID = 7L;
    // 설정에는 통째로, 콜백에는 뒷부분 숫자만 온다.
    private static final String CONFIGURED_AD_UNIT = "ca-app-pub-3940256099942544/1111111111";
    private static final String AD_UNIT = "1111111111";

    private static KeyPair googleKeyPair;
    private static KeyPair otherKeyPair;

    @Mock private AdRewardLogRepository adRewardLogRepository;
    @Mock private CurrencyLogRepository currencyLogRepository;
    @Mock private UserRepository userRepository;
    @Mock private RewardService rewardService;

    private AdRewardService adRewardService;

    @BeforeAll
    static void generateKeys() throws Exception {
        googleKeyPair = newKeyPair();
        otherKeyPair = newKeyPair();
    }

    @BeforeEach
    void setUp() throws Exception {
        RewardedAdsVerifier verifier = new RewardedAdsVerifier.Builder()
                .addVerifyingPublicKey(KEY_ID, (ECPublicKey) googleKeyPair.getPublic())
                .build();
        adRewardService = new AdRewardService(verifier, adRewardLogRepository, currencyLogRepository,
                userRepository, rewardService, List.of(CONFIGURED_AD_UNIT));

        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(adRewardLogRepository.existsByProviderTransactionId(any())).thenReturn(false);
        when(adRewardLogRepository.saveAndFlush(any(AdRewardLog.class))).thenAnswer(inv -> {
            AdRewardLog log = inv.getArgument(0);
            ReflectionTestUtils.setField(log, "adRewardId", AD_REWARD_ID);
            return log;
        });
    }

    @Test
    @DisplayName("구글 서명이 맞고 우리 광고 단위면 AD_REWARD 정책으로 지급하고 기록을 REWARDED 로 남긴다")
    void grantsCoinsForSignedCallback() throws Exception {
        when(rewardService.grantPolicyReward(USER_ID, RewardType.AD_REWARD, RewardSource.AD_REWARD,
                AD_REWARD_ID, null, null, null))
                .thenReturn(RewardLog.granted(USER_ID, 1L, RewardType.AD_REWARD, RewardSource.AD_REWARD,
                        AD_REWARD_ID, null, null, null, 0, 20, null, null));
        CurrencyLog currencyLog = mock(CurrencyLog.class);
        when(currencyLog.getCurrencyLogId()).thenReturn(99L);
        when(currencyLogRepository.findFirstByReferenceTypeAndReferenceId("AD_REWARD", AD_REWARD_ID))
                .thenReturn(Optional.of(currencyLog));

        String query = signed(googleKeyPair, unsignedQuery(AD_UNIT, "tx-1", "42"));
        adRewardService.handleAdMobCallback(query, AD_UNIT, "tx-1", "42");

        ArgumentCaptor<AdRewardLog> saved = ArgumentCaptor.forClass(AdRewardLog.class);
        verify(adRewardLogRepository).saveAndFlush(saved.capture());
        AdRewardLog log = saved.getValue();
        assertThat(log.getUserId()).isEqualTo(USER_ID);
        assertThat(log.getAdProvider()).isEqualTo("ADMOB");
        assertThat(log.getAdUnitId()).isEqualTo(AD_UNIT);
        assertThat(log.getProviderTransactionId()).isEqualTo("tx-1");
        assertThat(log.getRewardStatus()).isEqualTo(AdRewardStatus.REWARDED);
        assertThat(log.getCoinAmount()).isEqualTo(20);
        assertThat(log.getCurrencyLogId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("한도 초과로 지급되지 않으면 기록은 FAILED·0코인으로 남는다")
    void keepsFailedWhenRewardSkipped() throws Exception {
        when(rewardService.grantPolicyReward(USER_ID, RewardType.AD_REWARD, RewardSource.AD_REWARD,
                AD_REWARD_ID, null, null, null))
                .thenReturn(RewardLog.skipped(USER_ID, 1L, RewardType.AD_REWARD, RewardSource.AD_REWARD,
                        AD_REWARD_ID, null, null, null, RewardSkipReason.DAILY_LIMIT_EXCEEDED));

        String query = signed(googleKeyPair, unsignedQuery(AD_UNIT, "tx-6", "42"));
        adRewardService.handleAdMobCallback(query, AD_UNIT, "tx-6", "42");

        ArgumentCaptor<AdRewardLog> saved = ArgumentCaptor.forClass(AdRewardLog.class);
        verify(adRewardLogRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getRewardStatus()).isEqualTo(AdRewardStatus.FAILED);
        assertThat(saved.getValue().getCoinAmount()).isZero();
        verify(currencyLogRepository, never()).findFirstByReferenceTypeAndReferenceId(any(), anyLong());
    }

    @Test
    @DisplayName("서명 뒤에 user_id 를 바꾼 콜백은 거절하고 아무것도 저장하지 않는다")
    void rejectsTamperedCallback() throws Exception {
        String query = signed(googleKeyPair, unsignedQuery(AD_UNIT, "tx-2", "42"))
                .replace("user_id=42", "user_id=43");

        assertThatThrownBy(() -> adRewardService.handleAdMobCallback(query, AD_UNIT, "tx-2", "43"))
                .isInstanceOf(CoreMateException.class)
                .extracting(e -> ((CoreMateException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_AD_REWARD_SIGNATURE);
        verifyNothingGranted();
    }

    @Test
    @DisplayName("구글이 아닌 키로 서명한 콜백은 거절한다")
    void rejectsCallbackSignedWithOtherKey() throws Exception {
        String query = signed(otherKeyPair, unsignedQuery(AD_UNIT, "tx-3", "42"));

        assertThatThrownBy(() -> adRewardService.handleAdMobCallback(query, AD_UNIT, "tx-3", "42"))
                .isInstanceOf(CoreMateException.class);
        verifyNothingGranted();
    }

    @Test
    @DisplayName("서명이 없는 요청은 거절한다")
    void rejectsUnsignedCallback() {
        String query = unsignedQuery(AD_UNIT, "tx-4", "42");

        assertThatThrownBy(() -> adRewardService.handleAdMobCallback(query, AD_UNIT, "tx-4", "42"))
                .isInstanceOf(CoreMateException.class);
        assertThatThrownBy(() -> adRewardService.handleAdMobCallback(null, AD_UNIT, "tx-4", "42"))
                .isInstanceOf(CoreMateException.class);
        verifyNothingGranted();
    }

    @Test
    @DisplayName("이미 처리한 transaction_id 가 다시 오면(구글 재전송) 다시 지급하지 않는다")
    void ignoresRedeliveredCallback() throws Exception {
        when(adRewardLogRepository.existsByProviderTransactionId("tx-5")).thenReturn(true);

        String query = signed(googleKeyPair, unsignedQuery(AD_UNIT, "tx-5", "42"));
        adRewardService.handleAdMobCallback(query, AD_UNIT, "tx-5", "42");

        verifyNothingGranted();
    }

    @Test
    @DisplayName("서명은 맞아도 우리 광고 단위가 아니면 지급하지 않는다")
    void ignoresOtherAdUnit() throws Exception {
        String query = signed(googleKeyPair, unsignedQuery("2222222222", "tx-7", "42"));
        adRewardService.handleAdMobCallback(query, "2222222222", "tx-7", "42");

        verifyNothingGranted();
    }

    @Test
    @DisplayName("user_id 가 없거나 모르는 사용자면 지급하지 않는다")
    void ignoresUnknownUser() throws Exception {
        String withoutUser = signed(googleKeyPair, "ad_network=5450213213286189855&ad_unit=" + AD_UNIT
                + "&reward_amount=20&reward_item=coin&timestamp=1761100000000&transaction_id=tx-8");
        adRewardService.handleAdMobCallback(withoutUser, AD_UNIT, "tx-8", null);

        String unknownUser = signed(googleKeyPair, unsignedQuery(AD_UNIT, "tx-9", "999"));
        adRewardService.handleAdMobCallback(unknownUser, AD_UNIT, "tx-9", "999");

        verifyNothingGranted();
    }

    private void verifyNothingGranted() {
        verify(adRewardLogRepository, never()).saveAndFlush(any());
        verify(rewardService, never()).grantPolicyReward(any(), any(), any(), any(), any(), any(), any());
    }

    // 구글 콜백과 같은 순서의 파라미터. signature·key_id 는 signed 가 끝에 붙인다.
    private static String unsignedQuery(String adUnit, String transactionId, String userId) {
        return "ad_network=5450213213286189855&ad_unit=" + adUnit + "&reward_amount=20&reward_item=coin"
                + "&timestamp=1761100000000&transaction_id=" + transactionId + "&user_id=" + userId;
    }

    private static String signed(KeyPair keyPair, String unsignedQuery) throws Exception {
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(keyPair.getPrivate());
        signer.update(unsignedQuery.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(signer.sign());
        return unsignedQuery + "&signature=" + signature + "&key_id=" + KEY_ID;
    }

    private static KeyPair newKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }
}
