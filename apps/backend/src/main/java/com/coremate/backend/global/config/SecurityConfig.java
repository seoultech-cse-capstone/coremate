package com.coremate.backend.global.config;

import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.global.response.ErrorResponse;
import com.coremate.backend.global.security.JwtAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/login",
                                "/api/auth/oauth/*",
                                "/api/auth/signup",
                                "/api/auth/email/send-code",
                                "/api/auth/email/verify-code",
                                "/api/auth/reissue",
                                "/api/auth/find-email",
                                "/api/auth/password/reset-code",
                                "/api/auth/password/verify-code",
                                "/api/auth/password/reset"
                        ).permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                // 부하테스트 관측용(이슈 #273). Prometheus 가 인증 없이 메트릭을 긁는다.
                                // 로컬 127.0.0.1 바인딩이라 외부 노출은 없다.
                                "/actuator/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/solve-sessions/concept",
                                "/api/v1/solve-sessions/guest/complete"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/content/**",
                                "/api/v1/solve/questions/*/hint",
                                // 광고 시청 완료 콜백. 구글이 토큰 없이 부르고, 서비스가 서명으로 출처를 확인한다.
                                "/api/rewards/ads/admob-ssv"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");

                            // 기기 세션이 해지되어 막힌 경우에만 사유 코드를 실어 보낸다. 그래야 앱이
                            // "다른 기기에서 로그인"과 "그냥 토큰 만료"를 구분해 모달 표시 여부를 정한다.
                            // 사유가 없으면 기존 응답 형태를 그대로 유지한다(조용한 재발급 경로 보존).
                            Object failureCode =
                                    request.getAttribute(JwtAuthenticationFilter.AUTH_FAILURE_CODE_ATTRIBUTE);
                            Object body = failureCode instanceof ErrorCode errorCode
                                    ? ErrorResponse.of(errorCode)
                                    : unauthenticatedBody();

                            response.getWriter().write(objectMapper.writeValueAsString(body));
                        })
                        .accessDeniedHandler((request, response, e) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write(objectMapper.writeValueAsString(
                                    Map.of("success", false, "message", "접근 권한이 없습니다.", "data", null)
                            ));
                        })
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * 세션 해지가 아닌 평범한 인증 실패(토큰 없음·서명 오류·만료)에 쓰는 기존 응답 형태.
     *
     * <p>{@code Map.of} 는 null 값을 받으면 NPE 를 던진다. 그동안 이 자리에서
     * {@code Map.of(..., "data", null)} 이 매번 터져 응답이 Tomcat 기본 HTML 에러 페이지로
     * 나가고 있었다. 클라이언트가 본문의 code 로 "다른 기기에서 로그인"과 "단순 만료"를 구분해야
     * 하므로 두 경로 모두 JSON 이어야 한다.
     */
    private static Map<String, Object> unauthenticatedBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", "인증이 필요합니다.");
        body.put("data", null);
        return body;
    }
}
