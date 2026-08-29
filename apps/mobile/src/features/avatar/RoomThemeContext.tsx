import * as SecureStore from "expo-secure-store";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";

import {
  DEFAULT_ROOM_THEME_KEY,
  ROOM_THEME_BY_KEY,
  ROOM_THEMES,
  type RoomTheme,
  type RoomThemeKey,
} from "./roomThemes";

const ROOM_THEME_KEY = "coremate.roomThemeKey";

type RoomThemeContextValue = {
  themeKey: RoomThemeKey;
  theme: RoomTheme;
  setThemeKey: (key: RoomThemeKey) => void;
};

const RoomThemeContext = createContext<RoomThemeContextValue | null>(null);

function isRoomThemeKey(value: string | null): value is RoomThemeKey {
  return value !== null && ROOM_THEME_BY_KEY.has(value as RoomThemeKey);
}

// 마이페이지에서 고른 방 테마(벽지+바닥 세트)를 앱 전체(홈 화면 방 등)에서 같이 쓴다.
// 실제 방 API가 없어서 지금은 기기 저장소에만 남기는 로컬 상태다.
export function RoomThemeProvider({ children }: { children: ReactNode }) {
  const [themeKey, setThemeKeyState] = useState<RoomThemeKey>(DEFAULT_ROOM_THEME_KEY);

  useEffect(() => {
    let mounted = true;

    (async () => {
      try {
        const stored = await SecureStore.getItemAsync(ROOM_THEME_KEY);
        if (mounted && isRoomThemeKey(stored)) {
          setThemeKeyState(stored);
        }
      } catch (error) {
        console.error("[roomTheme] 저장된 방 테마 복원 실패", error);
      }
    })();

    return () => {
      mounted = false;
    };
  }, []);

  const setThemeKey = useCallback((next: RoomThemeKey) => {
    setThemeKeyState(next);
    SecureStore.setItemAsync(ROOM_THEME_KEY, next).catch((error) => {
      console.error("[roomTheme] 방 테마 저장 실패", error);
    });
  }, []);

  const value = useMemo<RoomThemeContextValue>(
    () => ({
      themeKey,
      theme: ROOM_THEME_BY_KEY.get(themeKey) ?? ROOM_THEMES[0],
      setThemeKey,
    }),
    [themeKey, setThemeKey],
  );

  return <RoomThemeContext.Provider value={value}>{children}</RoomThemeContext.Provider>;
}

export function useRoomTheme(): RoomThemeContextValue {
  const ctx = useContext(RoomThemeContext);
  if (!ctx) {
    throw new Error("useRoomTheme은 RoomThemeProvider 내부에서만 사용할 수 있습니다.");
  }
  return ctx;
}
