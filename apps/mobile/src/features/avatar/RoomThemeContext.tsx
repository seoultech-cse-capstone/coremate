import { useQueryClient } from "@tanstack/react-query";
import { createContext, useCallback, useContext, useMemo, type ReactNode } from "react";

import { useAuth } from "../auth";
import { cancelBeforeOptimisticUpdate } from "./avatarWrites";
import { useUpdateEquippedItemsMutation } from "./useAvatarQueries";
import { useInventory } from "./useInventoryQueries";
import { useMyRoom } from "./useRoomQueries";
import type { RoomResponse } from "./roomTypes";
import {
  ROOM_THEME_BY_KEY,
  ROOM_THEME_ITEM_STEM,
  ROOM_THEMES,
  themeKeyFromEquipped,
  themeKeyFromImageUrl,
  type RoomTheme,
  type RoomThemeKey,
} from "./roomThemes";

type RoomThemeContextValue = {
  themeKey: RoomThemeKey;
  theme: RoomTheme;
  setThemeKey: (key: RoomThemeKey) => void;
};

const RoomThemeContext = createContext<RoomThemeContextValue | null>(null);

const ROOM_QUERY_KEY = ["avatar", "room"] as const;
const WALLPAPER_SLOT = "WALLPAPER";

// 마이페이지에서 고른 방 테마(벽지+바닥 세트)를 앱 전체(홈 화면 방 등)에서 같이 쓴다.
// 테마는 서버의 WALLPAPER 장착 슬롯이며(equipped-items), react-query 방 캐시에서 파생한다.
export function RoomThemeProvider({ children }: { children: ReactNode }) {
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();
  const roomQuery = useMyRoom(accessToken);
  const inventoryQuery = useInventory(accessToken);
  const equipMutation = useUpdateEquippedItemsMutation(accessToken);

  // 서버 장착의 WALLPAPER → 테마 key. 없으면 기본 테마.
  const themeKey = useMemo<RoomThemeKey>(
    () => themeKeyFromEquipped(roomQuery.data?.equippedItems ?? []),
    [roomQuery.data],
  );

  // 테마 key → 보유 WALLPAPER userItemId.
  const themeToUserItemId = useMemo(() => {
    const map = new Map<RoomThemeKey, number>();
    for (const group of inventoryQuery.data?.groups ?? []) {
      for (const item of group.items) {
        const key = themeKeyFromImageUrl(item.imageUrl);
        if (key) {
          map.set(key, item.userItemId);
        }
      }
    }
    return map;
  }, [inventoryQuery.data]);

  const setThemeKey = useCallback(
    async (next: RoomThemeKey) => {
      const userItemId = themeToUserItemId.get(next);
      // 보유하지 않은 테마는 장착할 수 없다(목록에 안 뜨지만 방어).
      if (userItemId === undefined) {
        return;
      }

      await cancelBeforeOptimisticUpdate(queryClient, ROOM_QUERY_KEY);
      // 방 캐시의 WALLPAPER 장착을 낙관적으로 교체해 화면 즉시 반영(themeKey가 여기서 파생).
      queryClient.setQueryData<RoomResponse>(ROOM_QUERY_KEY, (old) => {
        if (!old) {
          return old;
        }
        const imageUrl = `/assets/room/${ROOM_THEME_ITEM_STEM[next]}.png`;
        const others = old.equippedItems.filter((item) => item.slot !== WALLPAPER_SLOT);
        const prev = old.equippedItems.find((item) => item.slot === WALLPAPER_SLOT);
        return {
          ...old,
          equippedItems: [
            ...others,
            {
              slot: WALLPAPER_SLOT,
              userItemId,
              itemId: prev?.itemId ?? 0,
              itemName: prev?.itemName ?? "",
              itemType: "WALLPAPER",
              rarity: prev?.rarity ?? "COMMON",
              imageUrl,
              renderOrder: prev?.renderOrder ?? null,
            },
          ],
        };
      });

      equipMutation.mutate({ equippedItems: [{ slot: WALLPAPER_SLOT, userItemId }] });
    },
    [themeToUserItemId, queryClient, equipMutation],
  );

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
