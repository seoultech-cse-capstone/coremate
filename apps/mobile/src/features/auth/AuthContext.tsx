import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";

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
      try {
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
      } catch {
        if (mounted) {
          setStatus("unauthenticated");
        }
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
