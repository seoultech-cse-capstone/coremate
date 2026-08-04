// 백엔드 auth 도메인 기기 세션 계약과 1:1로 대응하는 타입.
// (com.coremate.backend.auth.dto.response)

/** GET /api/auth/sessions 의 항목 */
export type DeviceSession = {
  sessionId: string;
  deviceName: string;
  platform: string;
  osVersion: string | null;
  lastSeenAt: string;
  /** 최초 등록 시각. IP·위치를 수집하지 않으므로 본인 기기 판별의 유일한 근거다. */
  createdAt: string;
  /** 지금 이 앱이 쓰는 기기인지. 서버가 토큰의 세션 식별자와 비교해 계산한다. */
  current: boolean;
};

/** GET /api/auth/sessions 응답 */
export type SessionListResponse = {
  sessions: DeviceSession[];
};
