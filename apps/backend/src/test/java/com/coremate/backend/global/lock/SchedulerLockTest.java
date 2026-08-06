package com.coremate.backend.global.lock;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class SchedulerLockTest {

    @Autowired
    private SchedulerLock schedulerLock;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void secondAcquireOnSameKeyFails() {
        String key = "test:" + UUID.randomUUID();
        try {
            assertThat(schedulerLock.tryAcquire(key, Duration.ofSeconds(30))).isTrue();

            // 다른 인스턴스가 같은 시각에 같은 작업을 시작한 상황.
            assertThat(schedulerLock.tryAcquire(key, Duration.ofSeconds(30))).isFalse();
        } finally {
            stringRedisTemplate.delete("scheduler:lock:" + key);
        }
    }

    @Test
    void differentKeysDoNotBlockEachOther() {
        String today = "test:" + UUID.randomUUID();
        String tomorrow = "test:" + UUID.randomUUID();
        try {
            // 잠금 키에 날짜가 들어가므로 다음 날 같은 작업은 정상적으로 다시 돌아야 한다.
            assertThat(schedulerLock.tryAcquire(today, Duration.ofSeconds(30))).isTrue();
            assertThat(schedulerLock.tryAcquire(tomorrow, Duration.ofSeconds(30))).isTrue();
        } finally {
            stringRedisTemplate.delete("scheduler:lock:" + today);
            stringRedisTemplate.delete("scheduler:lock:" + tomorrow);
        }
    }
}
