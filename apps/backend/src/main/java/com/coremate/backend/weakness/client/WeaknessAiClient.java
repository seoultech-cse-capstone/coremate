package com.coremate.backend.weakness.client;

import com.coremate.backend.weakness.config.WeaknessAnalysisProperties;
import com.coremate.backend.weakness.dto.ai.AiWeaknessAnalysisRequest;
import com.coremate.backend.weakness.dto.ai.AiWeaknessAnalysisResponse;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Python AI 서버(ai-pipeline)의 약점분석 엔드포인트를 호출하는 HTTP 클라이언트.
 * 요청/응답은 snake_case(AI DTO의 @JsonProperty). 호출 실패는 예외로 전파해 상위 트랜잭션이 롤백되게 한다.
 */
@Component
public class WeaknessAiClient {

    private static final String ANALYZE_PATH = "/api/v1/weakness/analyze";

    private final RestClient restClient;

    public WeaknessAiClient(WeaknessAnalysisProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(props.getAiTimeoutSec() * 1000);
        this.restClient = RestClient.builder()
                .baseUrl(props.getAiServerBaseUrl())
                .requestFactory(factory)
                .build();
    }

    public AiWeaknessAnalysisResponse analyze(AiWeaknessAnalysisRequest request) {
        return restClient.post()
                .uri(ANALYZE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AiWeaknessAnalysisResponse.class);
    }
}
