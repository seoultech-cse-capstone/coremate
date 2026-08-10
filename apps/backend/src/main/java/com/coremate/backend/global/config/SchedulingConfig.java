package com.coremate.backend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// 정기 알림 발송에 쓰는 스케줄러를 켠다.
// 컨테이너 타임존이 Asia/Seoul 이지만 cron 표현식에도 zone 을 명시해, 다른 타임존에서
// 띄워도 발송 시각이 흔들리지 않게 한다.
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
