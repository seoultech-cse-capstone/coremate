package com.coremate.backend.auth.oauth;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.coremate.backend.auth.event.UserWithdrawnEvent;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * 탈퇴한 회원의 카카오 연결 끊기 요청 모양을 확인한다. 카카오 서버 대신 MockRestServiceServer 를 쓴다.
 */
class KakaoUnlinkClientTest {

    private static final String UNLINK_URI = "https://kapi.kakao.com/v1/user/unlink";
    private static final String ADMIN_KEY = "test-admin-key";

    private final RestClient.Builder restClientBuilder = RestClient.builder();
    private final MockRestServiceServer kakaoServer = MockRestServiceServer.bindTo(restClientBuilder).build();

    @Test
    void 탈퇴_이벤트의_카카오_회원번호로_어드민_키를_써서_연결을_끊는다() {
        KakaoUnlinkClient client = new KakaoUnlinkClient(restClientBuilder, ADMIN_KEY);
        kakaoServer.expect(requestTo(UNLINK_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "KakaoAK " + ADMIN_KEY))
                .andExpect(content().formDataContains(Map.of(
                        "target_id_type", "user_id",
                        "target_id", "1234567890")))
                .andRespond(withSuccess());

        client.unlinkOnWithdraw(new UserWithdrawnEvent(1L, List.of(), List.of("1234567890")));

        kakaoServer.verify();
    }

    @Test
    void 연결_끊기에_실패해도_예외를_던지지_않는다() {
        KakaoUnlinkClient client = new KakaoUnlinkClient(restClientBuilder, ADMIN_KEY);
        kakaoServer.expect(requestTo(UNLINK_URI)).andRespond(withBadRequest());

        client.unlinkOnWithdraw(new UserWithdrawnEvent(1L, List.of(), List.of("1234567890")));

        kakaoServer.verify();
    }

    @Test
    void 어드민_키가_없거나_카카오로_가입하지_않았으면_요청하지_않는다() {
        new KakaoUnlinkClient(restClientBuilder, "")
                .unlinkOnWithdraw(new UserWithdrawnEvent(1L, List.of(), List.of("1234567890")));
        new KakaoUnlinkClient(restClientBuilder, ADMIN_KEY)
                .unlinkOnWithdraw(new UserWithdrawnEvent(1L, List.of("apple-refresh"), List.of()));

        kakaoServer.verify();
    }
}
