import * as SecureStore from "expo-secure-store";
import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from "react";

export type ContentLanguage = "ko" | "en";

const LANGUAGE_KEY = "coremate.contentLanguage";

type LanguageContextValue = {
  language: ContentLanguage;
  setLanguage: (language: ContentLanguage) => void;
};

const LanguageContext = createContext<LanguageContextValue | null>(null);

export function LanguageProvider({ children }: { children: ReactNode }) {
  const [language, setLanguageState] = useState<ContentLanguage>("ko");

  // 언어 설정 메뉴를 막아 둔 동안(#248)은 저장된 언어를 복원하지 않고 한국어로 시작한다.
  // 예전에 English 를 고른 사용자는 메뉴가 막혀 되돌릴 길이 없기 때문이다. 메뉴를 다시 열 때
  // SecureStore.getItemAsync(LANGUAGE_KEY) 로 복원하던 효과를 되살린다.

  const setLanguage = useCallback((next: ContentLanguage) => {
    setLanguageState(next);
    SecureStore.setItemAsync(LANGUAGE_KEY, next).catch((error) => {
      console.error("[language] 언어 설정 저장 실패", error);
    });
  }, []);

  const value = useMemo<LanguageContextValue>(
    () => ({ language, setLanguage }),
    [language, setLanguage],
  );

  return (
    <LanguageContext.Provider value={value}>
      {children}
    </LanguageContext.Provider>
  );
}

export function useLanguage(): LanguageContextValue {
  const ctx = useContext(LanguageContext);
  if (!ctx) {
    throw new Error("useLanguage는 LanguageProvider 내부에서만 사용할 수 있습니다.");
  }
  return ctx;
}
