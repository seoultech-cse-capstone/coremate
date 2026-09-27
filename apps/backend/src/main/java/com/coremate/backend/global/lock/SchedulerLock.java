package com.coremate.backend.global.lock;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

// 스케줄 작업이 여러 인스턴스에서 동시에 도는 것을 막는 분산 락.
//
// Redis 의 SET NX 하나로 충분하다. 잠금을 먼저 잡은 인스턴스만 작업하고 나머지는 건너뛴다.
// 만료 시간을 두어, 작업 중 인스턴스가 죽어도 잠금이 영구히 남지 않게 한다.
//
// 잠금을 명시적으로 푸는 기능은 두지 않았다. 정기 작업은 "같은 날 한 번"이 목적이라
// 만료 시간이 곧 잠금 유효 기간이면 충분하고, 작업이 끝났다고 풀어 버리면 그 직후 다른
// 인스턴스가 같은 작업을 다시 할 수 있다.
@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerLock {

    private static final String KEY_PREFIX = "scheduler:lock:";

    private final StringRedisTemplate stringRedisTemplate;

    // 잠금을 잡으면 true. 이미 다른 인스턴스가 잡았으면 false.
    //
    // Redis 에 접근할 수 없으면 true 를 돌려준다(작업을 진행시킨다). 알림이 하루 통째로
    // 빠지는 것보다, 인스턴스가 여럿일 때 드물게 중복 발송되는 편이 낫다고 봤다. 발송 로그
    // 기준 중복 차단이 두 번째 방어선으로 남아 있어 완전히 무방비도 아니다.
    public boolean tryAcquire(String key, Duration ttl) {
        try {
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(KEY_PREFIX + key, "locked", ttl);
            return Boolean.TRUE.equals(acquired);
        } catch (Exception e) {
            log.error("스케줄 잠금 획득 실패. 잠금 없이 진행한다. key={}", key, e);
            return true;
        }
    }
}
