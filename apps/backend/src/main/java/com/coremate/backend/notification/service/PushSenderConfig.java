package com.coremate.backend.notification.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

// 실제 전송기와 로그 전송기 중 하나를 고른다.
//
// Firebase 자격증명은 git 에 올리지 않아 없는 환경이 있고, 테스트도 자격증명 없이 돈다.
// 조건부 빈 애너테이션을 겹쳐 쓰는 대신 여기서 한 번에 판단해, 어떤 구현이 왜 선택됐는지
// 로그 한 줄로 드러나게 했다.
//
// FirebaseConfig 의 초기화가 끝난 뒤에 판단해야 하므로 명시적으로 의존을 건다.
@Slf4j
@Configuration
@DependsOn("firebaseConfig")
public class PushSenderConfig {

    @Bean
    public PushSender pushSender() {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase 가 초기화되지 않아 푸시를 실제로 보내지 않는다. 발송 판단과 기록은 그대로 동작한다.");
            return new LoggingPushSender();
        }
        log.info("FCM 전송기를 사용한다.");
        return new FcmPushSender(FirebaseMessaging.getInstance());
    }
}
