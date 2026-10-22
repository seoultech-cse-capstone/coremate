package com.coremate.backend.reward.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coremate.backend.reward.entity.AdRewardLog;
import com.coremate.backend.reward.entity.CurrencyLog;
import com.coremate.backend.reward.entity.UserCurrency;
import com.coremate.backend.reward.entity.type.AdRewardStatus;
import com.coremate.backend.reward.repository.AdRewardLogRepository;
import com.coremate.backend.reward.repository.CurrencyLogRepository;
import com.coremate.backend.reward.repository.UserCurrencyRepository;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserRole;
import com.coremate.backend.user.repository.UserRepository;
import com.google.crypto.tink.apps.rewardedads.RewardedAdsVerifier;
import jakarta.persistence.EntityManager;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * GET /api/rewards/ads/admob-ssv 를 인증 필터부터 코인 잔액까지 실제로 태운다(H2, #363).
 *
 * <p>구글 공개키 대신 테스트 키로 검증하는 빈을 끼운다. 나머지(보안 설정·지급 정책·일일 한도)는 실제 그대로다.
 */
@SpringBootTest(properties = "admob.rewarded-ad-unit-ids=ca-app-pub-3940256099942544/1111111111")
@AutoConfigureMockMvc
@Transactional
@DisplayName("광고 시청 완료 콜백 API")
class AdRewardCallbackApiTest {

    private static final long KEY_ID = 1234L;
    private static final String AD_UNIT = "1111111111";
    private static final KeyPair GOOGLE_KEY_PAIR = newKeyPair();

    @TestConfiguration
    static class TestVerifierConfig {

        @Bean
        @Primary
        RewardedAdsVerifier testRewardedAdsVerifier() throws GeneralSecurityException {
            return new RewardedAdsVerifier.Builder()
                    .addVerifyingPublicKey(KEY_ID, (ECPublicKey) GOOGLE_KEY_PAIR.getPublic())
                    .build();
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private UserCurrencyRepository userCurrencyRepository;
    @Autowired private AdRewardLogRepository adRewardLogRepository;
    @Autowired private CurrencyLogRepository currencyLogRepository;
    @Autowired private EntityManager entityManager;

    private Long userId;

    @BeforeEach
    void seed() {
        userId = userRepository.save(User.builder()
                .email("ad-reward@test.com").passwordHash("hash").name("광고")
                .nickname("광고보상").phoneNumber("01055556666").role(UserRole.USER).build()).getUserId();
        // H2 에서는 정책 시드(R__01)가 돌지 않는다. 운영 시드와 같은 값으로 넣는다.
        entityManager.createNativeQuery("""
                INSERT INTO reward_policies (reward_type, reward_kind, coin_amount, xp_amount, daily_limit,
                    min_correct_rate, first_reward_only, scope_daily_once, reward_scope, description, is_active,
                    created_at, updated_at)
                VALUES ('AD_REWARD', 'COIN', 20, 0, 5, NULL, FALSE, FALSE, 'PER_DAY', '광고 시청 보상', TRUE,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """).executeUpdate();
    }

    @Test
    @DisplayName("토큰 없이 온 구글 서명 콜백으로 20코인을 주고, 같은 콜백이 다시 와도 한 번만 준다")
    void grantsOnceForSignedCallback() throws Exception {
        String query = signedQuery("tx-1");

        callback(query).andExpect(status().isOk());
        callback(query).andExpect(status().isOk());

        assertThat(coin()).isEqualTo(20);
        assertThat(adRewardLogRepository.count()).isEqualTo(1);
        AdRewardLog log = adRewardLogRepository.findAll().getFirst();
        assertThat(log.getRewardStatus()).isEqualTo(AdRewardStatus.REWARDED);
        assertThat(log.getCoinAmount()).isEqualTo(20);
        assertThat(currencyLogRepository.findById(log.getCurrencyLogId()))
                .get()
                .extracting(CurrencyLog::getReferenceId)
                .isEqualTo(log.getAdRewardId());
    }

    @Test
    @DisplayName("하루 다섯 번까지만 주고 여섯 번째는 FAILED 로 남긴다")
    void stopsAtDailyLimit() throws Exception {
        for (int i = 1; i <= 6; i++) {
            callback(signedQuery("tx-limit-" + i)).andExpect(status().isOk());
        }

        assertThat(coin()).isEqualTo(100);
        assertThat(adRewardLogRepository.findAll(Sort.by("adRewardId")))
                .extracting(AdRewardLog::getRewardStatus)
                .containsExactly(AdRewardStatus.REWARDED, AdRewardStatus.REWARDED, AdRewardStatus.REWARDED,
                        AdRewardStatus.REWARDED, AdRewardStatus.REWARDED, AdRewardStatus.FAILED);
    }

    @Test
    @DisplayName("서명이 틀리면 400 INVALID_AD_REWARD_SIGNATURE 이고 아무것도 남기지 않는다")
    void rejectsBadSignature() throws Exception {
        String query = signedQuery("tx-bad").replace("user_id=" + userId, "user_id=" + (userId + 1));

        callback(query)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AD_REWARD_SIGNATURE"));

        assertThat(adRewardLogRepository.count()).isZero();
        assertThat(coin()).isZero();
    }

    // URI 로 넘겨야 쿼리 문자열이 템플릿 처리 없이 그대로 간다. 서명은 원문 그대로에 걸려 있다.
    private ResultActions callback(String query) throws Exception {
        return mockMvc.perform(get(URI.create("/api/rewards/ads/admob-ssv?" + query)));
    }

    private int coin() {
        return userCurrencyRepository.findById(userId).map(UserCurrency::getCoin).orElse(0);
    }

    // 구글 콜백과 같은 순서로 만들고 끝에 signature·key_id 를 붙인다.
    private String signedQuery(String transactionId) throws Exception {
        String unsigned = "ad_network=5450213213286189855&ad_unit=" + AD_UNIT + "&reward_amount=20"
                + "&reward_item=coin&timestamp=1761100000000&transaction_id=" + transactionId + "&user_id=" + userId;
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(GOOGLE_KEY_PAIR.getPrivate());
        signer.update(unsigned.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(signer.sign());
        return unsigned + "&signature=" + signature + "&key_id=" + KEY_ID;
    }

    private static KeyPair newKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
