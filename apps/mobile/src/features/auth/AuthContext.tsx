import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { AppState } from "react-native";

import { setAuthHandlers, type SessionRevokedCode } from "../../shared/api/httpClient";
// 배럴(index.ts)이 아니라 파일을 직접 가리킨다. features/device 의 훅이 useAuth 를
// 참조하므로, 배럴로 묶으면 모듈 순환이 생긴다.
import { deviceApi } from "../device/deviceApi";
import { tokenStorage } from "./tokenStorage";
import type { AuthSession, AuthUser } from "./types";

type AuthStatus = "loading" | "authenticated" | "guest" | "unauthenticated";

type AuthContextValue = {
  /** loading: 저장소 복원 중 / authenticated: 로그인됨 / guest: 게스트 / unauthenticated: 미로그인 */
  status: AuthStatus;
  user: AuthUser | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  /** 기기 세션이 해지되어 끊긴 사유. null이면 안내할 것이 없다. */
  revokedReason: SessionRevokedCode | null;
  /** 로그인/회원가입 성공 시 세션을 저장하고 인증 상태로 전환 */
  signIn: (session: AuthSession) => Promise<void>;
  /** 게스트로 계속하기(토큰 없이 제한된 기능 사용) */
  continueAsGuest: () => Promise<void>;
  /** 로그아웃: 저장된 세션을 지우고 미로그인 상태로 전환 */
  signOut: () => Promise<void>;
  /** 해지 안내를 확인한 뒤 호출해 상태를 비운다. */
  clearRevokedReason: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("loading");
  const [user, setUser] = useState<AuthUser | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [revokedReason, setRevokedReason] = useState<SessionRevokedCode | null>(null);

  // 앱 시작 시 저장소에서 세션을 복원한다.
  useEffect(() => {
    let mounted = true;

    (async () => {
      const session = await tokenStorage.load();
      if (!mounted) {
        return;
      }
      if (session) {
        setUser(session.user);
        setAccessToken(session.accessToken);
        setStatus("authenticated");
      } else {
        setStatus("unauthenticated");
      }
    })();

    return () => {
      mounted = false;
    };
  }, []);

  const signIn = useCallback(async (session: AuthSession) => {
    await tokenStorage.save(session);
    setUser(session.user);
    setAccessToken(session.accessToken);
    setRevokedReason(null);
    setStatus("authenticated");
  }, []);

  const continueAsGuest = useCallback(async () => {
    await tokenStorage.clear();
    setUser(null);
    setAccessToken(null);
    setStatus("guest");
  }, []);

  const signOut = useCallback(async () => {
    await tokenStorage.clear();
    setUser(null);
    setAccessToken(null);
    setStatus("unauthenticated");
  }, []);

  const clearRevokedReason = useCallback(() => {
    setRevokedReason(null);
  }, []);

  // 해지 사유를 남긴 뒤 로그아웃한다. 순서가 중요하다 — signOut이 먼저면
  // 모달이 뜨기 전에 상태가 초기화된다.
  const handleSessionRevoked = useCallback(
    async (code: SessionRevokedCode) => {
      setRevokedReason(code);
      await signOut();
    },
    [signOut],
  );

  // 액세스 토큰 만료(401) 시 httpClient가 재발급받은 토큰을 반영한다.
  const updateTokens = useCallback(
    async (nextAccessToken: string, nextRefreshToken: string) => {
      if (user) {
        await tokenStorage.save({
          accessToken: nextAccessToken,
          refreshToken: nextRefreshToken,
          user,
        });
      }
      setAccessToken(nextAccessToken);
    },
    [user],
  );

  // httpClient는 features/auth를 직접 참조할 수 없으므로(순환 참조 방지),
  // 실제 재발급/로그아웃 구현을 여기서 주입한다. user가 바뀐 렌더 커밋 직후
  // 곧바로(다른 비동기 작업이 끼어들기 전에) 최신 updateTokens로 교체되도록
  // useEffect가 아닌 useLayoutEffect를 사용한다.
  useLayoutEffect(() => {
    setAuthHandlers({
      getRefreshToken: () => tokenStorage.getRefreshToken(),
      onTokensRefreshed: updateTokens,
      // 단순 만료·네트워크 오류는 지금처럼 조용히 로그아웃만 한다.
      onRefreshFailed: signOut,
      onSessionRevoked: handleSessionRevoked,
    });

    return () => {
      setAuthHandlers(null);
    };
  }, [updateTokens, signOut, handleSessionRevoked]);

  // 백그라운드에 있는 동안 다른 기기에서 해지되면 앱은 그 사실을 모른다.
  // 포그라운드로 돌아올 때 한 번 확인해서 복귀 즉시 안내가 뜨게 한다.
  // 기기 목록 조회를 쓰는 이유는 해지 감지 + 마지막 접속 갱신을 한 번에 하기 위해서다.
  const accessTokenRef = useRef(accessToken);
  accessTokenRef.current = accessToken;

  useEffect(() => {
    if (status !== "authenticated") {
      return;
    }

    const subscription = AppState.addEventListener("change", (next) => {
      if (next !== "active") {
        return;
      }
      const token = accessTokenRef.current;
      if (!token) {
        return;
      }
      // 실패는 httpClient의 401 분기가 처리한다. 여기서는 결과를 쓰지 않는다.
      deviceApi.getSessions(token).catch(() => undefined);
    });

    return () => {
      subscription.remove();
    };
  }, [status]);

  const value = useMemo<AuthContextValue>(
    () => ({
      status,
      user,
      accessToken,
      isAuthenticated: status === "authenticated",
      revokedReason,
      signIn,
      continueAsGuest,
      signOut,
      clearRevokedReason,
    }),
    [
      status,
      user,
      accessToken,
      revokedReason,
      signIn,
      continueAsGuest,
      signOut,
      clearRevokedReason,
    ],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth는 AuthProvider 내부에서만 사용할 수 있습니다.");
  }
  return ctx;
}
