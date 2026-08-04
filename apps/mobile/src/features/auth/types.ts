// 백엔드 auth 도메인 계약과 1:1로 대응하는 타입.
// (com.coremate.backend.auth.dto)

export type LoginRequest = {
  email: string;
  password: string;
  // 기기 정보는 useLoginMutation이 채워 넣는다. 화면은 email/password만 넘기면 된다.
  deviceId?: string;
  deviceName?: string;
  platform?: string;
  osVersion?: string | null;
};

export type SignupRequest = {
  email: string;
  password: string;
  nickname: string;
  name: string;
  phoneNumber: string;
};

export type ReissueRequest = {
  refreshToken: string;
};

/** POST /api/auth/login 응답 */
export type LoginResponse = {
  accessToken: string;
  refreshToken: string;
  expiresInSec: number;
  userId: number;
  nickname: string;
  /** 서버가 확정한 기기 식별자. 이 값을 저장해 다음 로그인에 그대로 보낸다. */
  deviceId: string;
};

/** POST /api/auth/signup 응답 */
export type SignupResponse = {
  userId: number;
  email: string;
  nickname: string;
};

/** POST /api/auth/reissue 응답 */
export type AuthTokenResponse = {
  accessToken: string;
  refreshToken: string;
  expiresInSec: number;
};

// 프론트 내부에서 다루는 인증 세션 표현.

export type AuthUser = {
  userId: number;
  nickname: string;
};

export type AuthSession = {
  accessToken: string;
  refreshToken: string;
  user: AuthUser;
};
