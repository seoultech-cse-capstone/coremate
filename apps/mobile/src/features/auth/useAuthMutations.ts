import { useMutation } from "@tanstack/react-query";

import { authApi } from "./authApi";
import { useAuth } from "./AuthContext";
import { tokenStorage } from "./tokenStorage";
import type { LoginRequest, SignupRequest, SignupResponse } from "./types";

/**
 * 로그인 → 성공 시 세션 저장 + 인증 상태 전환.
 * 화면에서는 mutate({ email, password })만 호출하면 된다.
 */
export function useLoginMutation() {
  const { signIn } = useAuth();

  return useMutation({
    mutationFn: (body: LoginRequest) => authApi.login(body),
    onSuccess: async (res) => {
      await signIn({
        accessToken: res.accessToken,
        refreshToken: res.refreshToken,
        user: { userId: res.userId, nickname: res.nickname },
      });
    },
  });
}

/**
 * 회원가입. 백엔드가 토큰을 주지 않으므로(가입=인증 아님) 자동 로그인은 하지 않는다.
 * 가입 성공 후 화면에서 로그인으로 유도한다.
 */
export function useSignupMutation() {
  return useMutation<SignupResponse, unknown, SignupRequest>({
    mutationFn: (body) => authApi.signup(body),
  });
}

/**
 * 로그아웃. 서버에 리프레시 토큰 무효화를 요청하되,
 * 서버 호출 성공/실패와 무관하게 로컬 세션은 반드시 정리한다.
 */
export function useLogoutMutation() {
  const { signOut } = useAuth();

  return useMutation({
    mutationFn: async () => {
      // accessToken이 이미 만료된 상태면 httpClient가 요청 도중 백그라운드로
      // 토큰을 재발급받아버려서, 처음에 보낸 refreshToken은 서버 기준으로
      // 이미 교체된 값이 된다. 재발급이 실제로 일어났는지 확인해 최신
      // refreshToken으로 한 번 더 무효화를 시도한다.
      const attemptLogout = async () => {
        const [refreshToken, accessToken] = await Promise.all([
          tokenStorage.getRefreshToken(),
          tokenStorage.getAccessToken(),
        ]);
        if (!refreshToken || !accessToken) {
          return null;
        }
        try {
          await authApi.logout(refreshToken, accessToken);
        } catch {
          // 서버 로그아웃 실패는 무시하고 로컬만 정리한다.
        }
        return refreshToken;
      };

      const sentRefreshToken = await attemptLogout();
      if (sentRefreshToken) {
        const latestRefreshToken = await tokenStorage.getRefreshToken();
        if (latestRefreshToken && latestRefreshToken !== sentRefreshToken) {
          await attemptLogout();
        }
      }
    },
    onSettled: async () => {
      await signOut();
    },
  });
}
