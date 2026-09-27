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

export type ContentLanguage = "ko" | "en";

const LANGUAGE_KEY = "coremate.contentLanguage";

type LanguageContextValue = {
  language: ContentLanguage;
  setLanguage: (language: ContentLanguage) => void;
};

const LanguageContext = createContext<LanguageContextValue | null>(null);

export function LanguageProvider({ children }: { children: ReactNode }) {
  const [language, setLanguageState] = useState<ContentLanguage>("ko");

  // 앱 시작 시 저장된 언어 설정을 복원한다.
  useEffect(() => {
    let mounted = true;

    (async () => {
      try {
        const stored = await SecureStore.getItemAsync(LANGUAGE_KEY);
        if (mounted && (stored === "ko" || stored === "en")) {
          setLanguageState(stored);
        }
      } catch (error) {
        console.error("[language] 저장된 언어 설정 복원 실패", error);
      }
    })();

    return () => {
      mounted = false;
    };
  }, []);

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
