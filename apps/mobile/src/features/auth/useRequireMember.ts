import { router } from "expo-router";
import { useEffect } from "react";

import { useAuth } from "./AuthContext";

/**
 * 회원 전용 화면 가드. 탭바를 거치지 않고 직접 진입해도(딥링크 등)
 * 게스트/비로그인 상태면 로그인 필요 안내로 되돌린다.
 */
export function useRequireMember(): void {
  const { status } = useAuth();

  useEffect(() => {
    if (status === "guest" || status === "unauthenticated") {
      router.replace("/login-required");
    }
  }, [status]);
}
