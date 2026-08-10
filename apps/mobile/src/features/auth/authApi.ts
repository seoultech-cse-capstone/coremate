import { httpClient } from "../../shared/api/httpClient";
import type {
  AuthTokenResponse,
  LoginRequest,
  LoginResponse,
  ReissueRequest,
  SendSignupCodeRequest,
  SignupCodeResponse,
  SignupRequest,
  SignupResponse,
  SignupVerifyResponse,
  VerifySignupCodeRequest,
} from "./types";

/**
 * auth 도메인 REST 호출. 화면/훅은 이 모듈만 통해 서버와 통신한다.
 */
export const authApi = {
  login(body: LoginRequest): Promise<LoginResponse> {
    return httpClient.post<LoginResponse>("/api/auth/login", body);
  },

  signup(body: SignupRequest): Promise<SignupResponse> {
    return httpClient.post<SignupResponse>("/api/auth/signup", body);
  },

  sendSignupCode(body: SendSignupCodeRequest): Promise<SignupCodeResponse> {
    return httpClient.post<SignupCodeResponse>("/api/auth/email/send-code", body);
  },

  verifySignupCode(body: VerifySignupCodeRequest): Promise<SignupVerifyResponse> {
    return httpClient.post<SignupVerifyResponse>("/api/auth/email/verify-code", body);
  },

  reissue(body: ReissueRequest): Promise<AuthTokenResponse> {
    return httpClient.post<AuthTokenResponse>("/api/auth/reissue", body);
  },

  logout(refreshToken: string, accessToken: string): Promise<void> {
    return httpClient.post<void>("/api/auth/logout", { refreshToken }, { accessToken });
  },
};
