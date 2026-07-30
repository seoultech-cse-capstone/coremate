import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useLayoutEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";

import { setAuthHandlers } from "../../shared/api/httpClient";
import { tokenStorage } from "./tokenStorage";
import type { AuthSession, AuthUser } from "./types";

type AuthStatus = "loading" | "authenticated" | "guest" | "unauthenticated";

type AuthContextValue = {
  /** loading: 저장소 복원 중 / authenticated: 로그인됨 / guest: 게스트 / unauthenticated: 미로그인 */
  status: AuthStatus;
  user: AuthUser | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  /** 로그인/회원가입 성공 시 세션을 저장하고 인증 상태로 전환 */
  signIn: (session: AuthSession) => Promise<void>;
  /** 게스트로 계속하기(토큰 없이 제한된 기능 사용) */
  continueAsGuest: () => Promise<void>;
  /** 로그아웃: 저장된 세션을 지우고 미로그인 상태로 전환 */
  signOut: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("loading");
  const [user, setUser] = useState<AuthUser | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(null);

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
      onRefreshFailed: signOut,
    });

    return () => {
      setAuthHandlers(null);
    };
  }, [updateTokens, signOut]);

  const value = useMemo<AuthContextValue>(
    () => ({
      status,
      user,
      accessToken,
      isAuthenticated: status === "authenticated",
      signIn,
      continueAsGuest,
      signOut,
    }),
    [status, user, accessToken, signIn, continueAsGuest, signOut],
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
