package com.coremate.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class WeaknessAnalysisAsyncConfig {

    // 약점분석 생성의 "② AI 호출 + ③ 저장" 을 요청 스레드 밖에서 돌리기 위한 전용 풀(#323).
    // 분석표는 과금되는 의도적 동작이라 동시 요청이 폭주하지 않으므로 풀을 작게 둔다. 코어를 넘는
    // 요청은 큐에 쌓고, 큐까지 차면 AbortPolicy(기본)로 거절한다 — 거절은 서비스가 받아
    // 코인 환불·FAILED 로 보상한다(요청 스레드를 붙잡는 CallerRuns 를 피한다).
    @Bean
    public ThreadPoolTaskExecutor weaknessAnalysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("weakness-ai-");
        // 기동 종료 시 진행 중인 분석은 마무리하게 둬서 GENERATING 상태로 방치되는 일을 줄인다.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
