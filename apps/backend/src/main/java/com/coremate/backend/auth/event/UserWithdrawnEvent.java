package com.coremate.backend.auth.event;

import java.util.List;

/**
 * 회원 탈퇴가 처리됐다는 이벤트. 탈퇴가 커밋된 뒤 Apple 연결을 끊는다({@code AppleTokenClient}).
 *
 * <p>탈퇴하면서 소셜 연결 행을 지우므로, 해지에 필요한 Apple refresh token 은 지우기 전에 담아 넘긴다.
 * Apple 로 가는 HTTP 요청을 탈퇴 트랜잭션 밖에서 하려고 이벤트로 나눴다.
 *
 * @param appleRefreshTokens 해지할 Apple refresh token. Apple 로 로그인한 적이 없으면 비어 있다.
 */
public record UserWithdrawnEvent(Long userId, List<String> appleRefreshTokens) {
}
