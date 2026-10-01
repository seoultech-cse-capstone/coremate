import { useQueryClient } from "@tanstack/react-query";
import { createContext, useCallback, useContext, useMemo, type ReactNode } from "react";

import { useAuth } from "../auth";
import { cancelBeforeOptimisticUpdate } from "./avatarWrites";
import { toEquippedMap } from "./itemStem";
import type { LocalAvatarSlot } from "./localCatalog";
import type { UserAvatar } from "./types";
import { useMyAvatar } from "./useAvatarQueries";

/** 부위별로 착용 중인 로컬 카탈로그 아이템 key. 값이 null이면 "미착용"을 뜻하고,
 * 부위 자체가 없으면 기본 착용(DEFAULT_LOCAL_KEY_BY_SLOT)을 그대로 쓴다는 뜻이다. */
export type EquippedMap = Partial<Record<LocalAvatarSlot, string | null>>;

type AvatarEquipContextValue = {
  equipped: EquippedMap;
  setEquippedItem: (slot: LocalAvatarSlot, key: string | null) => void;
};

const AvatarEquipContext = createContext<AvatarEquipContextValue | null>(null);

const AVATAR_ME_QUERY_KEY = ["avatar", "me"] as const;

// 내 아바타의 착용 상태를 앱 전체(홈 화면 방, 마이페이지, 인벤토리)에서 같이 쓴다.
// 서버 장착(GET /api/avatar/me)이 원본이며 react-query 캐시에서 파생한다 — 방 테마·가구
// 컨텍스트와 같은 구조다. 예전에는 기기 저장소에 따로 들고 있었는데, 계정별로 나뉘지 않고
// 서버와도 어긋나서(새 기기에서는 기본 모습, 같은 기기에서 계정을 바꾸면 이전 계정 옷) 없앴다.
export function AvatarEquipProvider({ children }: { children: ReactNode }) {
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();
  const avatarQuery = useMyAvatar(accessToken);

  const equipped = useMemo<EquippedMap>(
    () => toEquippedMap(avatarQuery.data?.profile.equippedItems ?? []),
    [avatarQuery.data],
  );

  // 화면을 바로 바꾸기 위한 낙관적 갱신이다. 서버 저장은 호출부(인벤토리)가 하고, 성공하면
  // 서버 응답이 이 캐시를 덮어쓰고(뒤에 줄 선 쓰기가 없을 때만) 실패하면 다시 불러와 되돌린다
  // (useUpdateEquippedItemsMutation).
  const setEquippedItem = useCallback(
    async (slot: LocalAvatarSlot, key: string | null) => {
      await cancelBeforeOptimisticUpdate(queryClient, AVATAR_ME_QUERY_KEY);
      queryClient.setQueryData<UserAvatar>(AVATAR_ME_QUERY_KEY, (old) => {
        if (!old) {
          return old;
        }
        const others = old.profile.equippedItems.filter((item) => item.slot !== slot);
        // 서버 응답이 곧 덮어쓰는 임시 값이라, 화면이 읽는 파일명(key)만 맞춘다.
        const equippedItems =
          key === null
            ? others
            : [
                ...others,
                {
                  slot,
                  userItemId: 0,
                  itemId: 0,
                  itemName: "",
                  itemType: slot,
                  rarity: "COMMON",
                  imageUrl: `/assets/avatar/${key}.png`,
                  renderOrder: null,
                },
              ];
        return { ...old, profile: { ...old.profile, equippedItems } };
      });
    },
    [queryClient],
  );

  const value = useMemo<AvatarEquipContextValue>(
    () => ({ equipped, setEquippedItem }),
    [equipped, setEquippedItem],
  );

  return <AvatarEquipContext.Provider value={value}>{children}</AvatarEquipContext.Provider>;
}

export function useAvatarEquip(): AvatarEquipContextValue {
  const ctx = useContext(AvatarEquipContext);
  if (!ctx) {
    throw new Error("useAvatarEquip은 AvatarEquipProvider 내부에서만 사용할 수 있습니다.");
  }
  return ctx;
}
