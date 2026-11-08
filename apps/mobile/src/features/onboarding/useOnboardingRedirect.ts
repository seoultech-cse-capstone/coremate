import { router } from "expo-router";
import { useEffect } from "react";

import { useAuth } from "../auth";
import { onboardingStorage } from "./onboardingStorage";

/** 로그인한 회원이 아직 온보딩을 끝내지 않았으면 온보딩 화면으로 보낸다. 게스트는 대상이 아니다. */
export function useOnboardingRedirect() {
  const { status, user } = useAuth();
  const userId = user?.userId ?? null;

  useEffect(() => {
    if (status !== "authenticated" || userId === null) {
      return;
    }
    let cancelled = false;
    onboardingStorage.isDone(userId).then((done) => {
      if (!cancelled && !done) {
        router.replace("/onboarding");
      }
    });
    return () => {
      cancelled = true;
    };
  }, [status, userId]);
}
