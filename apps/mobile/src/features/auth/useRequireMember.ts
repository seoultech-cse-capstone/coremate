import { router, useFocusEffect } from "expo-router";
import { useCallback } from "react";

import { useAuth } from "./AuthContext";

/**
 * 회원 전용 화면 가드. 탭바를 거치지 않고 직접 진입해도(딥링크 등)
 * 게스트/비로그인 상태면 로그인 필요 안내로 되돌린다.
 *
 * useFocusEffect를 써서 "이 화면이 실제로 보이는 동안"만 검사한다.
 * 일반 useEffect를 쓰면, 이 화면이 스택 아래에 마운트된 채로 남아있을 때
 * (예: 마이페이지 → 설정 화면으로 이동한 상태) 로그아웃으로 status가
 * 바뀌는 순간 화면에 보이지도 않는 이 화면이 리다이렉트를 쏴서, 로그아웃이
 * 원래 이동하려던 화면을 가로채는 문제가 있었다.
 */
export function useRequireMember(): void {
  const { status } = useAuth();

  useFocusEffect(
    useCallback(() => {
      if (status === "guest" || status === "unauthenticated") {
        router.replace("/login-required");
      }
    }, [status]),
  );
}
