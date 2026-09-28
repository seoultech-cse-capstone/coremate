package com.coremate.backend.global.handler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 매핑이 없는 경로는 404 다. 핸들러가 없으면 NoResourceFoundException 이 catch-all 로 떨어져
 * 500 "서버 오류"가 나간다 — 실제로 폐쇄된 API(PATCH /api/users/me/nickname)를 부른 앱 화면이
 * 서버 장애처럼 보였다. 인증 필터를 타지 않는 공개 경로(/api/v1/content/**) 아래에서 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("없는 경로는 404 다")
class NoHandlerFoundTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("매핑 없는 경로")
    void unknownPath() throws Exception {
        mockMvc.perform(get("/api/v1/content/no-such-endpoint"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
