import { useMutation } from "@tanstack/react-query";

import { getDeviceInfo } from "../device/deviceInfo";
import { deviceStorage } from "../device/deviceStorage";
import { unregisterPushToken } from "../notification/notificationApi";
import { authApi } from "./authApi";
import { useAuth } from "./AuthContext";
import { signInWithGoogle } from "./googleAuth";
import { signInWithKakao } from "./kakaoAuth";
import { signInWithNaver } from "./naverAuth";
import { tokenStorage } from "./tokenStorage";
import type {
  LoginRequest,
  SendSignupCodeRequest,
  SignupCodeResponse,
  SignupRequest,
  SignupResponse,
  SignupVerifyResponse,
  VerifySignupCodeRequest,
} from "./types";

/**
 * 로그인 → 성공 시 세션 저장 + 인증 상태 전환.
 * 화면에서는 mutate({ email, password })만 호출하면 된다.
 */
export function useLoginMutation() {
  const { signIn } = useAuth();

  return useMutation({
    mutationFn: async (body: LoginRequest) => {
      // 기기 정보는 화면이 아니라 여기서 채운다. 저장된 식별자가 없으면 보내지 않고
      // 서버가 새로 발급한 값을 응답에서 받는다.
      const [deviceId, deviceInfo] = [await deviceStorage.get(), getDeviceInfo()];
      return authApi.login({
        ...body,
        ...(deviceId ? { deviceId } : {}),
        deviceName: deviceInfo.deviceName,
        platform: deviceInfo.platform,
        osVersion: deviceInfo.osVersion,
      });
    },
    onSuccess: async (res) => {
      if (res.deviceId) {
        await deviceStorage.save(res.deviceId);
      }
      await signIn({
        accessToken: res.accessToken,
        refreshToken: res.refreshToken,
        user: { userId: res.userId, nickname: res.nickname },
      });
    },
  });
}

/**
 * 카카오 로그인 → SDK 토큰을 백엔드로 넘겨 세션 발급.
 * 응답이 일반 로그인과 동일하므로 성공 처리(세션 저장·상태 전환)도 그대로 재사용한다.
 * 화면에서는 mutate()만 호출하면 된다(취소·실패는 onError 로 받는다).
 */
export function useKakaoLoginMutation() {
  const { signIn } = useAuth();

  return useMutation({
    mutationFn: async () => {
      const accessToken = await signInWithKakao();
      const [deviceId, deviceInfo] = [await deviceStorage.get(), getDeviceInfo()];
      return authApi.oauthLogin("kakao", {
        accessToken,
        ...(deviceId ? { deviceId } : {}),
        deviceName: deviceInfo.deviceName,
        platform: deviceInfo.platform,
        osVersion: deviceInfo.osVersion,
      });
    },
    onSuccess: async (res) => {
      if (res.deviceId) {
        await deviceStorage.save(res.deviceId);
      }
      await signIn({
        accessToken: res.accessToken,
        refreshToken: res.refreshToken,
        user: { userId: res.userId, nickname: res.nickname },
      });
    },
  });
}

/**
 * 네이버 로그인 → SDK 토큰을 백엔드로 넘겨 세션 발급.
 * 카카오와 동일하게 응답이 일반 로그인과 같으므로 성공 처리도 그대로 재사용한다.
 * 화면에서는 mutate()만 호출하면 된다(취소·실패는 onError 로 받는다).
 */
export function useNaverLoginMutation() {
  const { signIn } = useAuth();

  return useMutation({
    mutationFn: async () => {
      const accessToken = await signInWithNaver();
      const [deviceId, deviceInfo] = [await deviceStorage.get(), getDeviceInfo()];
      return authApi.oauthLogin("naver", {
        accessToken,
        ...(deviceId ? { deviceId } : {}),
        deviceName: deviceInfo.deviceName,
        platform: deviceInfo.platform,
        osVersion: deviceInfo.osVersion,
      });
    },
    onSuccess: async (res) => {
      if (res.deviceId) {
        await deviceStorage.save(res.deviceId);
      }
      await signIn({
        accessToken: res.accessToken,
        refreshToken: res.refreshToken,
        user: { userId: res.userId, nickname: res.nickname },
      });
    },
  });
}

/**
 * 구글 로그인 → SDK 가 준 idToken 을 백엔드로 넘겨 세션 발급.
 * 백엔드는 idToken 을 검증하지만, 공통 요청 포맷을 위해 accessToken 도 함께 보낸다.
 * 성공 처리(세션 저장·상태 전환)는 다른 소셜 로그인과 동일하다.
 */
export function useGoogleLoginMutation() {
  const { signIn } = useAuth();

  return useMutation({
    mutationFn: async () => {
      const { idToken, accessToken } = await signInWithGoogle();
      const [deviceId, deviceInfo] = [await deviceStorage.get(), getDeviceInfo()];
      return authApi.oauthLogin("google", {
        accessToken,
        idToken,
        ...(deviceId ? { deviceId } : {}),
        deviceName: deviceInfo.deviceName,
        platform: deviceInfo.platform,
        osVersion: deviceInfo.osVersion,
      });
    },
    onSuccess: async (res) => {
      if (res.deviceId) {
        await deviceStorage.save(res.deviceId);
      }
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

/** 회원가입 이메일 인증코드 발송. */
export function useSendSignupCodeMutation() {
  return useMutation<SignupCodeResponse, unknown, SendSignupCodeRequest>({
    mutationFn: (body) => authApi.sendSignupCode(body),
  });
}

/** 회원가입 이메일 인증코드 확인. */
export function useVerifySignupCodeMutation() {
  return useMutation<SignupVerifyResponse, unknown, VerifySignupCodeRequest>({
    mutationFn: (body) => authApi.verifySignupCode(body),
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

      // 기기 토큰 해제를 세션 해지보다 먼저 한다. 순서가 뒤바뀌면 해제 요청이 이미
      // 무효해진 토큰으로 나가 401 로 실패하고, 서버에는 활성 토큰이 남아 로그아웃한
      // 기기로도 알림이 계속 간다.
      await unregisterPushToken();

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
