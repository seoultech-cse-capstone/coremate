package com.coremate.backend.reward.service;

import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.reward.entity.AdRewardLog;
import com.coremate.backend.reward.entity.CurrencyLog;
import com.coremate.backend.reward.entity.RewardLog;
import com.coremate.backend.reward.entity.type.RewardSource;
import com.coremate.backend.reward.entity.type.RewardStatus;
import com.coremate.backend.reward.entity.type.RewardType;
import com.coremate.backend.reward.repository.AdRewardLogRepository;
import com.coremate.backend.reward.repository.CurrencyLogRepository;
import com.coremate.backend.user.repository.UserRepository;
import com.google.crypto.tink.apps.rewardedads.RewardedAdsVerifier;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 광고 시청 보상(FR-PAY-02). AdMob 보상형 광고의 서버 측 확인(SSV) 콜백을 받아 코인을 지급한다.
 *
 * <p>앱이 "다 봤다"고 알리는 API 로 지급하면 그 API 만 직접 불러도 코인이 나간다. 그래서 시청이 끝나면
 * 구글이 우리 서버로 보내는 서명된 콜백만 믿는다. 앱은 광고를 띄울 때 사용자 ID 를 넘기고, 구글이 그 값을
 * 콜백의 user_id 로 돌려준다. 지급량과 하루 5회 한도는 다른 보상과 같이 reward_policies(AD_REWARD)가 정한다.
 *
 * <p>서명이 맞는 콜백은 지급하지 않더라도 정상 처리로 끝낸다. 200 이 아니면 구글이 같은 콜백을 최대 다섯 번
 * 다시 보내는데, 우리 광고 단위가 아니거나 사용자를 모르는 건은 다시 받아도 결과가 같기 때문이다.
 */
@Slf4j
@Service
public class AdRewardService {

    static final String AD_PROVIDER_ADMOB = "ADMOB";

    // 서명 검증 라이브러리가 URI 를 받아 쿼리 문자열만 쓴다. 그 형식을 맞추려고 붙이는 자리 표시 주소다.
    private static final String CALLBACK_URI_PREFIX = "https://admob-ssv.invalid/?";

    private final RewardedAdsVerifier rewardedAdsVerifier;
    private final AdRewardLogRepository adRewardLogRepository;
    private final CurrencyLogRepository currencyLogRepository;
    private final UserRepository userRepository;
    private final RewardService rewardService;
    private final Set<String> rewardedAdUnitIds;

    public AdRewardService(RewardedAdsVerifier rewardedAdsVerifier,
                           AdRewardLogRepository adRewardLogRepository,
                           CurrencyLogRepository currencyLogRepository,
                           UserRepository userRepository,
                           RewardService rewardService,
                           @Value("${admob.rewarded-ad-unit-ids:}") List<String> rewardedAdUnitIds) {
        this.rewardedAdsVerifier = rewardedAdsVerifier;
        this.adRewardLogRepository = adRewardLogRepository;
        this.currencyLogRepository = currencyLogRepository;
        this.userRepository = userRepository;
        this.rewardService = rewardService;
        // 콜백의 ad_unit 은 "ca-app-pub-…/1234567890" 의 뒷부분 숫자만 온다. 설정은 어느 쪽으로 적어도 되게 맞춘다.
        this.rewardedAdUnitIds = rewardedAdUnitIds.stream()
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .map(id -> id.substring(id.lastIndexOf('/') + 1))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * @param rawQueryString 받은 그대로의 쿼리 문자열. 서명은 파라미터 순서까지 포함해 원문에 걸려 있다.
     */
    @Transactional
    public void handleAdMobCallback(String rawQueryString, String adUnit, String transactionId, String userIdParam) {
        verifySignature(rawQueryString);

        if (adUnit == null || !rewardedAdUnitIds.contains(adUnit)) {
            log.warn("우리 보상형 광고 단위가 아닌 콜백이라 지급하지 않는다. adUnit={} transactionId={}",
                    adUnit, transactionId);
            return;
        }
        Long userId = parseUserId(userIdParam);
        if (userId == null || !userRepository.existsById(userId)) {
            log.warn("광고 보상 콜백의 사용자를 찾을 수 없어 지급하지 않는다. userId={} transactionId={}",
                    userIdParam, transactionId);
            return;
        }
        if (transactionId == null || transactionId.isBlank()) {
            log.warn("광고 보상 콜백에 transaction_id 가 없어 지급하지 않는다. userId={}", userId);
            return;
        }
        // 구글이 재전송한 콜백. 동시에 두 번 들어오면 아래 저장이 UNIQUE 에 막혀 한쪽이 실패하고,
        // 구글이 그 건을 다시 보내면 여기서 걸러진다.
        if (adRewardLogRepository.existsByProviderTransactionId(transactionId)) {
            return;
        }

        AdRewardLog adRewardLog = adRewardLogRepository.saveAndFlush(
                AdRewardLog.received(userId, AD_PROVIDER_ADMOB, adUnit, transactionId));
        RewardLog rewardLog = rewardService.grantPolicyReward(userId, RewardType.AD_REWARD, RewardSource.AD_REWARD,
                adRewardLog.getAdRewardId(), null, null, null);
        if (rewardLog.getRewardStatus() == RewardStatus.GRANTED) {
            Long currencyLogId = currencyLogRepository
                    .findFirstByReferenceTypeAndReferenceId(RewardSource.AD_REWARD.name(), adRewardLog.getAdRewardId())
                    .map(CurrencyLog::getCurrencyLogId)
                    .orElse(null);
            adRewardLog.markRewarded(rewardLog.getCoinAmount(), currencyLogId);
        }
    }

    private void verifySignature(String rawQueryString) {
        if (rawQueryString == null || rawQueryString.isBlank()) {
            throw new CoreMateException(ErrorCode.INVALID_AD_REWARD_SIGNATURE);
        }
        try {
            rewardedAdsVerifier.verify(CALLBACK_URI_PREFIX + rawQueryString);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            log.warn("광고 보상 콜백의 서명을 확인하지 못했다. {}", e.getMessage());
            throw new CoreMateException(ErrorCode.INVALID_AD_REWARD_SIGNATURE);
        }
    }

    private Long parseUserId(String userIdParam) {
        if (userIdParam == null) {
            return null;
        }
        try {
            return Long.valueOf(userIdParam);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
