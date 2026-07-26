import * as SecureStore from "expo-secure-store";

import type { AuthSession, AuthUser } from "./types";

// SecureStore 키는 영숫자, ".", "-", "_" 만 허용된다.
const ACCESS_TOKEN_KEY = "coremate.accessToken";
const REFRESH_TOKEN_KEY = "coremate.refreshToken";
const USER_KEY = "coremate.user";

// SecureStore가 예외를 던지면(기기 키스토어 손상 등) 호출부가 매번 방어하지 않도록
// 여기서 흡수하고 "저장 실패/값 없음"으로 취급한다.
async function setItem(key: string, value: string): Promise<void> {
  try {
    await SecureStore.setItemAsync(key, value);
  } catch {
    // no-op: 이번 세션은 메모리 상태로 계속 진행하고, 재시작 시 재로그인으로 자연 복구된다.
  }
}

async function getItem(key: string): Promise<string | null> {
  try {
    return await SecureStore.getItemAsync(key);
  } catch {
    return null;
  }
}

async function deleteItem(key: string): Promise<void> {
  try {
    await SecureStore.deleteItemAsync(key);
  } catch {
    // no-op
  }
}

/**
 * 액세스/리프레시 토큰과 최소 사용자 정보를 기기 보안 저장소에 보관한다.
 */
export const tokenStorage = {
  async save(session: AuthSession): Promise<void> {
    await Promise.all([
      setItem(ACCESS_TOKEN_KEY, session.accessToken),
      setItem(REFRESH_TOKEN_KEY, session.refreshToken),
      setItem(USER_KEY, JSON.stringify(session.user)),
    ]);
  },

  async load(): Promise<AuthSession | null> {
    const [accessToken, refreshToken, userRaw] = await Promise.all([
      getItem(ACCESS_TOKEN_KEY),
      getItem(REFRESH_TOKEN_KEY),
      getItem(USER_KEY),
    ]);

    if (!accessToken || !refreshToken || !userRaw) {
      return null;
    }

    try {
      const user = JSON.parse(userRaw) as AuthUser;
      return { accessToken, refreshToken, user };
    } catch {
      return null;
    }
  },

  async clear(): Promise<void> {
    await Promise.all([
      deleteItem(ACCESS_TOKEN_KEY),
      deleteItem(REFRESH_TOKEN_KEY),
      deleteItem(USER_KEY),
    ]);
  },

  getAccessToken(): Promise<string | null> {
    return getItem(ACCESS_TOKEN_KEY);
  },

  getRefreshToken(): Promise<string | null> {
    return getItem(REFRESH_TOKEN_KEY);
  },
};
