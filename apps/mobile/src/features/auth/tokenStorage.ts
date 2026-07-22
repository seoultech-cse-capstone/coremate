import * as SecureStore from "expo-secure-store";
import { Platform } from "react-native";

import type { AuthSession, AuthUser } from "./types";

// SecureStore 키는 영숫자, ".", "-", "_" 만 허용된다.
const ACCESS_TOKEN_KEY = "coremate.accessToken";
const REFRESH_TOKEN_KEY = "coremate.refreshToken";
const USER_KEY = "coremate.user";

const isWeb = Platform.OS === "web";

// 웹(expo web)에서는 SecureStore를 쓸 수 없어 localStorage로 대체한다.
const webStorage: Pick<Storage, "getItem" | "setItem" | "removeItem"> | null =
  isWeb && typeof globalThis !== "undefined" && (globalThis as { localStorage?: Storage }).localStorage
    ? (globalThis as unknown as { localStorage: Storage }).localStorage
    : null;

async function setItem(key: string, value: string): Promise<void> {
  if (isWeb) {
    webStorage?.setItem(key, value);
    return;
  }
  await SecureStore.setItemAsync(key, value);
}

async function getItem(key: string): Promise<string | null> {
  if (isWeb) {
    return webStorage?.getItem(key) ?? null;
  }
  return SecureStore.getItemAsync(key);
}

async function deleteItem(key: string): Promise<void> {
  if (isWeb) {
    webStorage?.removeItem(key);
    return;
  }
  await SecureStore.deleteItemAsync(key);
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
