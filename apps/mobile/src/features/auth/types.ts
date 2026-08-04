// 백엔드 auth 도메인 계약과 1:1로 대응하는 타입.
// (com.coremate.backend.auth.dto)

export type LoginRequest = {
  email: string;
  password: string;
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
