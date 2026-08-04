import { httpClient } from "../../shared/api/httpClient";
import type { SessionListResponse } from "./types";

/**
 * 기기 세션 REST 호출. 전부 회원 전용이다.
 *
 * accessToken을 인자로 받는 것은 다른 도메인 API 모듈과 같은 컨벤션이다.
 * (features/auth를 import하지 않으므로 모듈 순환이 생기지 않는다)
 */
export const deviceApi = {
  getSessions(accessToken: string): Promise<SessionListResponse> {
    return httpClient.get<SessionListResponse>("/api/auth/sessions", { accessToken });
  },

  revokeSession(sessionId: string, accessToken: string): Promise<void> {
    return httpClient.delete<void>(`/api/auth/sessions/${sessionId}`, { accessToken });
  },

  revokeOtherSessions(accessToken: string): Promise<void> {
    return httpClient.post<void>("/api/auth/sessions/revoke-others", undefined, {
      accessToken,
    });
  },
};
