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

export type WithdrawRequest = {
  password: string;
};

export type FindEmailRequest = {
  name: string;
  phoneNumber: string;
};

/** POST /api/auth/find-email 응답 */
export type FindEmailResponse = {
  emails: string[];
};

export type SendPasswordResetCodeRequest = {
  email: string;
};

/** POST /api/auth/password/reset-code 응답 */
export type PasswordResetCodeResponse = {
  expiresInSec: number;
};

export type VerifyPasswordResetCodeRequest = {
  email: string;
  code: string;
};

/** POST /api/auth/password/verify-code 응답 */
export type PasswordResetTokenResponse = {
  resetToken: string;
  expiresInSec: number;
};

export type ResetPasswordRequest = {
  resetToken: string;
  newPassword: string;
};

export type SendSignupCodeRequest = {
  email: string;
};

/** POST /api/auth/email/send-code 응답 */
export type SignupCodeResponse = {
  expiresInSec: number;
};

export type VerifySignupCodeRequest = {
  email: string;
  code: string;
};

/** POST /api/auth/email/verify-code 응답 */
export type SignupVerifyResponse = {
  verified: boolean;
  expiresInSec: number;
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
