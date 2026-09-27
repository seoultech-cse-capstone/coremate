package com.coremate.backend.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

// FCM 발송에 쓰는 Firebase Admin SDK 초기화.
// 서비스 계정 파일이 없으면 초기화를 건너뛴다. 파일은 git 에 올리지 않으므로 아직 내려받지
// 않은 팀원도 있는데, 그 환경에서 기동 자체가 막히면 알림과 무관한 개발까지 멈춘다.
@Slf4j
@Configuration
public class FirebaseConfig {

    private final String credentialsPath;
    private final String credentialsJson;

    public FirebaseConfig(@Value("${firebase.credentials-path:}") String credentialsPath,
                          @Value("${firebase.credentials-json:}") String credentialsJson) {
        this.credentialsPath = credentialsPath;
        this.credentialsJson = credentialsJson;
    }

    @PostConstruct
    public void init() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        try (InputStream credentials = openCredentials()) {
            if (credentials == null) {
                return;
            }
            FirebaseApp app = FirebaseApp.initializeApp(FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentials))
                    .build());
            log.info("Firebase 초기화 완료. projectId={}", app.getOptions().getProjectId());
        } catch (IOException e) {
            log.error("Firebase 초기화 실패. 푸시 알림은 발송되지 않는다.", e);
        }
    }

    // 배포 환경(ECS)은 파일을 둘 곳이 없어 JSON 내용 자체를 시크릿으로 주입하고, 로컬은 파일 경로를 준다.
    // 둘 다 없으면 null 을 돌려 초기화를 건너뛴다.
    private InputStream openCredentials() throws IOException {
        if (!credentialsJson.isBlank()) {
            return new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8));
        }
        if (credentialsPath.isBlank()) {
            log.warn("firebase.credentials-json 과 credentials-path 가 모두 비어 있어 Firebase 초기화를 건너뛴다. 푸시 알림은 발송되지 않는다.");
            return null;
        }

        Path path = Path.of(credentialsPath);
        if (!Files.exists(path)) {
            log.warn("Firebase 서비스 계정 파일이 없어 초기화를 건너뛴다. 푸시 알림은 발송되지 않는다. path={}", path.toAbsolutePath());
            return null;
        }
        return Files.newInputStream(path);
    }
}
