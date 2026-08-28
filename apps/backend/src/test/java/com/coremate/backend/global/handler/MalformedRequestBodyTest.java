package com.coremate.backend.global.handler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 요청 본문이 읽히지 않을 때의 응답 상태를 고정한다.
 * 핸들러가 없으면 사용자 입력 오류가 500 INTERNAL_SERVER_ERROR 로 나가 장애로 오인된다.
 * 인증 필터를 타지 않는 공개 엔드포인트(로그인)로 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("잘못된 요청 본문은 400 이다")
class MalformedRequestBodyTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("깨진 JSON")
    void brokenJson() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("본문이 아예 없음")
    void emptyBody() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("필드 타입이 맞지 않음")
    void wrongFieldType() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":[1,2],\"password\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    // 파싱 실패 원인이 응답에 새어 나가면 내부 필드명·타입이 드러난다.
    @Test
    @DisplayName("파싱 실패 상세는 응답에 노출하지 않는다")
    void doesNotLeakParserDetail() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":[1,2],\"password\":\"x\"}"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다"));
    }
}
