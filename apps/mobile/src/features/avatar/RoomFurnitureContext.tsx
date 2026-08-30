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

import { ROOM_GRID_COLS, ROOM_GRID_ROWS } from "./roomGrid";
import {
  doFootprintsOverlap,
  FURNITURE_SETS,
  FURNITURE_TYPES,
  type FurniturePosition,
  type FurnitureType,
} from "./roomFurniture";

const STORAGE_KEY = "coremate.roomFurniturePlacements";

export type { FurniturePosition };

type Placements = Partial<Record<FurnitureType, FurniturePosition>>;

type RoomFurnitureContextValue = {
  placements: Placements;
  /**
   * position이 null이면 치운다. 격자 범위를 벗어나면 안쪽으로 당겨서 놓는다.
   * 다른 가구와 칸이 겹치면 놓지 않고 false를 돌려준다(치우기는 항상 true).
   */
  setPlacement: (type: FurnitureType, position: FurniturePosition | null) => boolean;
};

const RoomFurnitureContext = createContext<RoomFurnitureContextValue | null>(null);

function clampPosition(type: FurnitureType, position: FurniturePosition): FurniturePosition {
  const footprintCols = FURNITURE_SETS[type].footprintCols;
  const col = Math.min(Math.max(position.col, 0), ROOM_GRID_COLS - footprintCols);
  const row = Math.min(Math.max(position.row, 0), ROOM_GRID_ROWS - 1);
  return { row, col };
}

function isPlacements(value: unknown): value is Placements {
  return typeof value === "object" && value !== null;
}

// 마이페이지에서 놓은 가구 배치를 홈 화면 방과 공유한다. 방 배치 API가 없어서
// 지금은 기기 저장소에만 남기는 로컬 상태다.
export function RoomFurnitureProvider({ children }: { children: ReactNode }) {
  const [placements, setPlacements] = useState<Placements>({});

  useEffect(() => {
    let mounted = true;

    (async () => {
      try {
        const stored = await SecureStore.getItemAsync(STORAGE_KEY);
        if (!stored || !mounted) {
          return;
        }
        const parsed = JSON.parse(stored) as unknown;
        if (isPlacements(parsed)) {
          setPlacements(parsed);
        }
      } catch (error) {
        console.error("[roomFurniture] 저장된 가구 배치 복원 실패", error);
      }
    })();

    return () => {
      mounted = false;
    };
  }, []);

  const setPlacement = useCallback(
    (type: FurnitureType, position: FurniturePosition | null): boolean => {
      if (position) {
        const clamped = clampPosition(type, position);
        const overlaps = FURNITURE_TYPES.some((otherType) => {
          const otherPosition = placements[otherType];
          return otherType !== type && otherPosition && doFootprintsOverlap(type, clamped, otherType, otherPosition);
        });
        if (overlaps) {
          return false;
        }
      }

      setPlacements((prev) => {
        const next = { ...prev };
        if (position) {
          next[type] = clampPosition(type, position);
        } else {
          delete next[type];
        }
        SecureStore.setItemAsync(STORAGE_KEY, JSON.stringify(next)).catch((error) => {
          console.error("[roomFurniture] 가구 배치 저장 실패", error);
        });
        return next;
      });
      return true;
    },
    [placements],
  );

  const value = useMemo<RoomFurnitureContextValue>(
    () => ({ placements, setPlacement }),
    [placements, setPlacement],
  );

  return <RoomFurnitureContext.Provider value={value}>{children}</RoomFurnitureContext.Provider>;
}

export function useRoomFurniture(): RoomFurnitureContextValue {
  const ctx = useContext(RoomFurnitureContext);
  if (!ctx) {
    throw new Error("useRoomFurniture는 RoomFurnitureProvider 내부에서만 사용할 수 있습니다.");
  }
  return ctx;
}
