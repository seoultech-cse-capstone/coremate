import { router, useFocusEffect } from "expo-router";
import { useCallback } from "react";

import { useAuth } from "./AuthContext";

/**
 * 로그인/회원가입 화면 가드. 이미 로그인된 사용자가 진입하면 홈으로 되돌린다.
 * useRequireMember의 반대 방향이며, 같은 이유로 useFocusEffect를 쓴다(화면이
 * 스택 아래에 마운트된 채로 남아있는 동안 로그인 상태가 바뀌어도, 보이지도
 * 않는 이 화면이 리다이렉트를 쏘지 않도록).
 */
export function useRedirectIfAuthenticated(): void {
  const { status } = useAuth();

  useFocusEffect(
    useCallback(() => {
      if (status === "authenticated") {
        router.replace("/(tabs)/home");
      }
    }, [status]),
  );
}
