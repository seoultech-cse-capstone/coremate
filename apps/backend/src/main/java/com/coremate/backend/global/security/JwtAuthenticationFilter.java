package com.coremate.backend.global.security;

import com.coremate.backend.auth.service.UserSessionService;
import com.coremate.backend.global.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 액세스 토큰을 검증하고 기기 세션이 살아 있는지 확인한다.
 *
 * <p><b>이 필터는 응답을 직접 쓰지 않는다.</b> 막아야 할 요청이어도 SecurityContext 에 인증을
 * 세팅하지 않고 실패 사유만 request attribute 에 남긴 뒤 체인을 그대로 통과시킨다. 필터는 그 경로가
 * permitAll 인지 모르기 때문에, 여기서 401 을 쓰면 폐기된 토큰을 든 앱이 게스트 겸용 경로
 * (개념 문제집 풀이, 콘텐츠 탐색, 힌트 조회)를 호출할 때 이전에는 되던 요청이 끊긴다.
 * 인증이 필요한 경로만 SecurityConfig 의 authenticationEntryPoint 에 도달하고, 거기서 이
 * attribute 를 읽어 사유가 담긴 응답을 만든다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** entryPoint 가 읽어 갈 실패 사유. 없으면 기존과 동일한 "인증이 필요합니다" 응답이 나간다. */
    public static final String AUTH_FAILURE_CODE_ATTRIBUTE = "coremate.authFailureCode";

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService userDetailsService;
    private final UserSessionService userSessionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        // 토큰이 없거나 서명·만료 검증에 실패한 경우는 지금까지와 완전히 동일하게 그냥 통과시킨다.
        // attribute 를 남기지 않으므로 단순 만료는 code 없는 401 이 되고, 앱은 조용히 재발급을 시도한다.
        if (token != null && jwtProvider.validateToken(token)) {
            authenticate(request, token);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        String sessionId = jwtProvider.getSessionId(token);

        // 살아 있는 세션인지부터 확인한다. 해지된 세션이면 액세스 토큰의 남은 유효기간과 무관하게 막힌다.
        var failure = userSessionService.resolveAuthFailure(sessionId);
        if (failure.isPresent()) {
            request.setAttribute(AUTH_FAILURE_CODE_ATTRIBUTE, failure.get());
            return;
        }

        try {
            CustomUserDetails userDetails =
                    userDetailsService.loadUserById(jwtProvider.getUserId(token), sessionId);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (UsernameNotFoundException e) {
            // 세션은 살아 있지만 계정이 사라졌거나 정지·탈퇴된 경우.
            request.setAttribute(AUTH_FAILURE_CODE_ATTRIBUTE, ErrorCode.SESSION_EXPIRED);
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
