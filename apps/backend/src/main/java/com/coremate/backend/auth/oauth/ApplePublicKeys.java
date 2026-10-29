package com.coremate.backend.auth.oauth;

import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.Jwks;
import java.security.Key;
import java.security.PublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Apple 이 identity token 서명에 쓰는 공개키(JWKS)를 받아 두고 kid 로 찾아 준다.
 *
 * <p>Apple 은 키를 가끔 바꾸므로 처음 보는 kid 가 오면 목록을 다시 받는다. 없는 kid 를 담은 위조 토큰이 몰려도
 * Apple 로 요청이 쏟아지지 않도록 다시 받는 건 1분에 한 번까지만 한다.
 */
@Component
@Slf4j
public class ApplePublicKeys {

    private static final String KEYS_URI = "https://appleid.apple.com/auth/keys";
    private static final Duration MIN_REFRESH_INTERVAL = Duration.ofMinutes(1);

    private final Supplier<String> keySetFetcher;
    private final Clock clock;

    private volatile Map<String, PublicKey> keysById = Map.of();
    private Instant lastFetchedAt;

    public ApplePublicKeys() {
        this(() -> RestClient.create().get().uri(KEYS_URI).retrieve().body(String.class), Clock.systemUTC());
    }

    ApplePublicKeys(Supplier<String> keySetFetcher, Clock clock) {
        this.keySetFetcher = keySetFetcher;
        this.clock = clock;
    }

    /** kid 에 맞는 공개키. 없으면 null 이고, 호출부(jjwt)는 이를 서명 검증 실패로 처리한다. */
    public PublicKey find(String keyId) {
        if (keyId == null) {
            return null;
        }
        PublicKey key = keysById.get(keyId);
        if (key == null) {
            refreshIfAllowed();
            key = keysById.get(keyId);
        }
        return key;
    }

    private synchronized void refreshIfAllowed() {
        Instant now = clock.instant();
        if (lastFetchedAt != null && now.isBefore(lastFetchedAt.plus(MIN_REFRESH_INTERVAL))) {
            return;
        }
        lastFetchedAt = now;
        try {
            keysById = parse(keySetFetcher.get());
        } catch (RuntimeException e) {
            // 받기·파싱 실패면 기존 키를 그대로 두고, 이번 로그인은 서명 검증 실패로 끝난다.
            log.warn("Apple 공개키 목록을 받지 못했다: {}", e.getMessage());
        }
    }

    private static Map<String, PublicKey> parse(String json) {
        JwkSet keySet = Jwks.setParser().build().parse(json);
        Map<String, PublicKey> keys = new HashMap<>();
        for (Jwk<?> jwk : keySet.getKeys()) {
            Key key = jwk.toKey();
            if (jwk.getId() != null && key instanceof PublicKey publicKey) {
                keys.put(jwk.getId(), publicKey);
            }
        }
        return Map.copyOf(keys);
    }
}
