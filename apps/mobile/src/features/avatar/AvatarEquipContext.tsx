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

import type { LocalAvatarSlot } from "./localCatalog";

const STORAGE_KEY = "coremate.avatarEquipped";

/** 부위별로 착용 중인 로컬 카탈로그 아이템 key. 값이 null이면 "미착용"을 뜻하고,
 * 부위 자체가 없으면 기본 착용(DEFAULT_LOCAL_KEY_BY_SLOT)을 그대로 쓴다는 뜻이다. */
export type EquippedMap = Partial<Record<LocalAvatarSlot, string | null>>;

type AvatarEquipContextValue = {
  equipped: EquippedMap;
  setEquippedItem: (slot: LocalAvatarSlot, key: string | null) => void;
};

const AvatarEquipContext = createContext<AvatarEquipContextValue | null>(null);

function isEquippedMap(value: unknown): value is EquippedMap {
  return typeof value === "object" && value !== null;
}

// 인벤토리 화면(inventory.tsx, 아바타 탭)에서 고른 착용 상태를 앱 전체(홈 화면
// 아바타 등)에서 같이 쓴다. 실제 장착 저장 API가 아직 없어서 지금은 기기 저장소에만
// 남기는 로컬 상태다.
export function AvatarEquipProvider({ children }: { children: ReactNode }) {
  const [equipped, setEquipped] = useState<EquippedMap>({});

  useEffect(() => {
    let mounted = true;

    (async () => {
      try {
        const stored = await SecureStore.getItemAsync(STORAGE_KEY);
        if (!stored || !mounted) {
          return;
        }
        const parsed = JSON.parse(stored) as unknown;
        if (isEquippedMap(parsed)) {
          setEquipped(parsed);
        }
      } catch (error) {
        console.error("[avatarEquip] 저장된 착용 상태 복원 실패", error);
      }
    })();

    return () => {
      mounted = false;
    };
  }, []);

  const setEquippedItem = useCallback((slot: LocalAvatarSlot, key: string | null) => {
    setEquipped((prev) => {
      const next = { ...prev, [slot]: key };
      SecureStore.setItemAsync(STORAGE_KEY, JSON.stringify(next)).catch((error) => {
        console.error("[avatarEquip] 착용 상태 저장 실패", error);
      });
      return next;
    });
  }, []);

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
