package com.coremate.backend.auth.oauth;

import com.coremate.backend.auth.event.UserWithdrawnEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 탈퇴한 회원의 카카오 연결을 끊는다(카카오 「연결 끊기」 API).
 *
 * <p>카카오는 서비스에서 탈퇴하면 연결 끊기를 반드시 호출하라고 한다(카카오 디벨로퍼스 보안 가이드). 탈퇴할 때는
 * 회원의 카카오 토큰이 없으므로 앱 어드민 키와 카카오 회원번호로 서버에서 끊는다. 어드민 키가 설정되지 않은
 * 환경에서는 요청하지 않는다(탈퇴는 되고 연결 끊기만 빠진다).
 */
@Component
@Slf4j
public class KakaoUnlinkClient {

    private static final String UNLINK_URI = "https://kapi.kakao.com/v1/user/unlink";

    private final RestClient restClient;
    private final String adminKey;

    @Autowired
    public KakaoUnlinkClient(@Value("${oauth.kakao.admin-key:}") String adminKey) {
        this(RestClient.builder(), adminKey);
    }

    KakaoUnlinkClient(RestClient.Builder restClientBuilder, String adminKey) {
        this.restClient = restClientBuilder.build();
        this.adminKey = adminKey;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void unlinkOnWithdraw(UserWithdrawnEvent event) {
        if (event.kakaoUserIds().isEmpty()) {
            return;
        }
        if (!StringUtils.hasText(adminKey)) {
            log.warn("oauth.kakao.admin-key 가 없어 탈퇴 회원 {} 의 카카오 연결 끊기를 건너뛴다.", event.userId());
            return;
        }
        event.kakaoUserIds().forEach(this::unlink);
    }

    private void unlink(String kakaoUserId) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("target_id_type", "user_id");
        form.add("target_id", kakaoUserId);
        try {
            restClient.post()
                    .uri(UNLINK_URI)
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + adminKey)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            // 탈퇴는 이미 커밋됐다. 이미 끊긴 연결(카카오 쪽에서 먼저 끊음)도 여기로 온다.
            log.warn("카카오 연결 끊기 실패: {}", e.getMessage());
        }
    }
}
