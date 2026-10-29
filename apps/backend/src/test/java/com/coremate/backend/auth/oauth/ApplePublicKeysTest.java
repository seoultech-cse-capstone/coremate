package com.coremate.backend.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Jwts;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Apple 공개키 목록(JWKS)을 kid 로 찾고, 처음 보는 kid 일 때만 1분에 한 번까지 다시 받는지 확인한다.
 */
class ApplePublicKeysTest {

    private final KeyPair first = Jwts.SIG.RS256.keyPair().build();
    private final KeyPair second = Jwts.SIG.RS256.keyPair().build();
    private final TestClock clock = new TestClock();
    private final AtomicInteger fetchCount = new AtomicInteger();
    private final Deque<String> responses = new ArrayDeque<>();

    private final ApplePublicKeys publicKeys = new ApplePublicKeys(() -> {
        fetchCount.incrementAndGet();
        String response = responses.poll();
        if (response == null) {
            throw new IllegalStateException("network down");
        }
        return response;
    }, clock);

    @Test
    void kid_로_공개키를_찾고_다음부터는_받아_둔_목록을_쓴다() {
        responses.add(keySet(jwk("kid-1", first.getPublic())));

        PublicKey found = publicKeys.find("kid-1");
        PublicKey again = publicKeys.find("kid-1");

        assertThat(found).isEqualTo(first.getPublic());
        assertThat(again).isEqualTo(first.getPublic());
        assertThat(fetchCount).hasValue(1);
    }

    @Test
    void Apple_이_키를_바꾸면_처음_보는_kid_에서_목록을_다시_받는다() {
        responses.add(keySet(jwk("kid-1", first.getPublic())));
        responses.add(keySet(jwk("kid-1", first.getPublic()), jwk("kid-2", second.getPublic())));
        publicKeys.find("kid-1");

        clock.advance(Duration.ofMinutes(2));

        assertThat(publicKeys.find("kid-2")).isEqualTo(second.getPublic());
        assertThat(fetchCount).hasValue(2);
    }

    @Test
    void 없는_kid_가_몰려도_1분_안에는_다시_받지_않는다() {
        responses.add(keySet(jwk("kid-1", first.getPublic())));
        publicKeys.find("kid-1");

        assertThat(publicKeys.find("forged-1")).isNull();
        assertThat(publicKeys.find("forged-2")).isNull();

        assertThat(fetchCount).hasValue(1);
    }

    @Test
    void 목록을_받지_못하면_null_이고_받아_둔_키는_그대로_쓴다() {
        responses.add(keySet(jwk("kid-1", first.getPublic())));
        publicKeys.find("kid-1");
        clock.advance(Duration.ofMinutes(2));

        // 두 번째 받기는 실패한다(응답 없음).
        assertThat(publicKeys.find("kid-2")).isNull();
        assertThat(publicKeys.find("kid-1")).isEqualTo(first.getPublic());
    }

    // Apple https://appleid.apple.com/auth/keys 응답과 같은 모양.
    private static String keySet(String... keys) {
        return "{\"keys\":[" + String.join(",", keys) + "]}";
    }

    private static String jwk(String keyId, PublicKey key) {
        RSAPublicKey rsa = (RSAPublicKey) key;
        return "{\"kty\":\"RSA\",\"kid\":\"" + keyId + "\",\"use\":\"sig\",\"alg\":\"RS256\","
                + "\"n\":\"" + base64Url(rsa.getModulus()) + "\",\"e\":\"" + base64Url(rsa.getPublicExponent()) + "\"}";
    }

    private static String base64Url(BigInteger value) {
        byte[] bytes = value.toByteArray();
        // BigInteger 는 부호 비트 때문에 앞에 0 바이트를 붙일 수 있는데, JWK 는 부호 없는 값이라 뗀다.
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] unsigned = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, unsigned, 0, unsigned.length);
            bytes = unsigned;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static final class TestClock extends Clock {

        private Instant now = Instant.parse("2026-10-29T00:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
