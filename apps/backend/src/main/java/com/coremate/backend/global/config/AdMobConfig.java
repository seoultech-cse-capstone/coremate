package com.coremate.backend.global.config;

import com.google.crypto.tink.apps.rewardedads.RewardedAdsVerifier;
import java.security.GeneralSecurityException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdMobConfig {

    // 광고 시청 완료 콜백의 서명을 구글 공개키로 확인한다. 키는 처음 검증할 때 내려받아
    // 응답의 캐시 기간 동안 메모리에 두므로 구글의 키 교체도 따라간다. 빈을 만들 때는 내려받지 않는다.
    @Bean
    public RewardedAdsVerifier rewardedAdsVerifier() throws GeneralSecurityException {
        return new RewardedAdsVerifier.Builder()
                .fetchVerifyingPublicKeysWith(RewardedAdsVerifier.KEYS_DOWNLOADER_INSTANCE_PROD)
                .build();
    }
}
