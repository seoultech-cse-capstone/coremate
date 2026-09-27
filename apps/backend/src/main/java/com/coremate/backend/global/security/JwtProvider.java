package com.coremate.backend.global.security;

import com.coremate.backend.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtProvider {

    /** 기기 세션 식별자(user_sessions.session_id). 해지 판정의 기준 단위다. */
    public static final String SESSION_ID_CLAIM = "sid";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiry-sec}")
    private long accessTokenExpirySec;

    @Value("${jwt.refresh-token-expiry-sec}")
    private long refreshTokenExpirySec;

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(Long userId, UserRole role, String sessionId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role.name())
                .claim(SESSION_ID_CLAIM, sessionId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpirySec * 1000))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(Long userId, String sessionId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(SESSION_ID_CLAIM, sessionId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTokenExpirySec * 1000))
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    /** sid 클레임 도입 전에 발급된 토큰이면 null 이다. 호출부는 재로그인 유도로 처리한다. */
    public String getSessionId(String token) {
        return parseClaims(token).get(SESSION_ID_CLAIM, String.class);
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("만료된 JWT 토큰: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("유효하지 않은 JWT 토큰: {}", e.getMessage());
        }
        return false;
    }

    public long getAccessTokenExpirySec() {
        return accessTokenExpirySec;
    }

    public long getRefreshTokenExpirySec() {
        return refreshTokenExpirySec;
    }
}
