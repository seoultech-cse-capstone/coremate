import { useQueryClient } from "@tanstack/react-query";
import { useEffect, useRef } from "react";

import { useAuth } from "../auth";
import { useMyAvatar } from "./useAvatarQueries";

// 앱 진입(인증 완료) 시 아바타 프로필을 서버에서 한 번 보장하는 단일 지점이다.
// GET /api/avatar/me 가 getOrCreateProfile → ensureGranted(기본 6종 지급)를 유발하므로,
// 여기서 호출해 두면 어느 화면을 먼저 열든 지급이 끝난 상태가 된다. 화면에 그릴 것이
// 없어 null 을 반환한다. (PushRegistration 과 같은 패턴 — AuthProvider 안에서만 동작)
export function AvatarProfileSync() {
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();
  const avatarQuery = useMyAvatar(accessToken);
  const syncedRef = useRef(false);

  // 최초 /me 성공 = 기본 지급이 끝난 시점. 보유목록/방 쿼리는 지급 전에 빈 결과로
  // 먼저 캐시됐을 수 있고(staleTime 동안 그대로 유지됨), 그러면 화면이 비어 보인다.
  // 지급이 끝난 이 시점에 한 번 무효화해 다시 불러오게 한다.
  useEffect(() => {
    if (avatarQuery.isSuccess && !syncedRef.current) {
      syncedRef.current = true;
      queryClient.invalidateQueries({ queryKey: ["avatar", "inventory"] });
      queryClient.invalidateQueries({ queryKey: ["avatar", "room"] });
    }
  }, [avatarQuery.isSuccess, queryClient]);

  return null;
}
