package com.coremate.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class TransactionConfig {

    // 긴 외부 I/O(예: 약점분석 AI 호출)를 트랜잭션 경계 밖으로 빼기 위해, 코드에서 짧은
    // 트랜잭션을 명시적으로 여닫을 때 쓴다. 기본값(PROPAGATION_REQUIRED, readOnly=false)이다.
    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
