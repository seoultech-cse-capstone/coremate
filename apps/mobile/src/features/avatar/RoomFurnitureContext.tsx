import { useQueryClient } from "@tanstack/react-query";
import { createContext, useCallback, useContext, useMemo, type ReactNode } from "react";

import { useAuth } from "../auth";
import { useInventory } from "./useInventoryQueries";
import { useMyRoom, useSavePlacementsMutation } from "./useRoomQueries";
import type { RoomResponse } from "./roomTypes";
import { ROOM_GRID_COLS, ROOM_GRID_ROWS } from "./roomGrid";
import {
  doFootprintsOverlap,
  FURNITURE_ITEM_STEM,
  FURNITURE_SETS,
  FURNITURE_TYPES,
  type FurniturePosition,
  type FurnitureType,
} from "./roomFurniture";

export type { FurniturePosition };

type Placements = Partial<Record<FurnitureType, FurniturePosition>>;

type RoomFurnitureContextValue = {
  placements: Placements;
  /** 배치된 가구의 서버 placementId. 포즈 저장(PATCH /room/pose)이 이 값을 요구한다. */
  placementIdByType: Partial<Record<FurnitureType, number>>;
  /**
   * position이 null이면 치운다. 격자 범위를 벗어나면 안쪽으로 당겨서 놓는다.
   * 다른 가구와 칸이 겹치면 놓지 않고 false를 돌려준다(치우기는 항상 true).
   */
  setPlacement: (type: FurnitureType, position: FurniturePosition | null) => boolean;
};

const RoomFurnitureContext = createContext<RoomFurnitureContextValue | null>(null);

const ROOM_QUERY_KEY = ["avatar", "room"] as const;

// 서버 image_url 파일명 stem ↔ 로컬 FurnitureType. 서버 배치/보유목록과 로컬 가구를 잇는 키.
const STEM_TO_TYPE = new Map<string, FurnitureType>(
  FURNITURE_TYPES.map((type) => [FURNITURE_ITEM_STEM[type], type]),
);

function stemFromImageUrl(imageUrl: string | null): string | null {
  if (!imageUrl) {
    return null;
  }
  const fileName = imageUrl.split("/").pop();
  return fileName ? fileName.replace(/\.[^.]+$/, "") : null;
}

function typeFromImageUrl(imageUrl: string | null): FurnitureType | undefined {
  const stem = stemFromImageUrl(imageUrl);
  return stem ? STEM_TO_TYPE.get(stem) : undefined;
}

function clampPosition(type: FurnitureType, position: FurniturePosition): FurniturePosition {
  const footprintCols = FURNITURE_SETS[type].footprintCols;
  const col = Math.min(Math.max(position.col, 0), ROOM_GRID_COLS - footprintCols);
  const row = Math.min(Math.max(position.row, 0), ROOM_GRID_ROWS - 1);
  return { row, col };
}

// 마이페이지에서 놓은 가구 배치를 홈 화면 방과 공유한다. 서버(GET/PUT /api/avatar/room)가
// 원본이며, 배치는 react-query 캐시(["avatar","room"])에서 파생한다. 변경 시 캐시를
// 낙관적으로 갱신해 화면을 즉시 바꾸고, 서버에는 배치 전체를 통째로 저장한다(전체 교체 계약).
export function RoomFurnitureProvider({ children }: { children: ReactNode }) {
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();
  const roomQuery = useMyRoom(accessToken);
  const inventoryQuery = useInventory(accessToken);
  const savePlacements = useSavePlacementsMutation(accessToken);

  // 서버 방 응답의 배치 → 로컬 { type: {row, col} }. gridX=col, gridY=row.
  const placements = useMemo<Placements>(() => {
    const map: Placements = {};
    for (const placement of roomQuery.data?.placements ?? []) {
      const type = typeFromImageUrl(placement.imageUrl);
      if (type) {
        map[type] = { row: placement.gridY, col: placement.gridX };
      }
    }
    return map;
  }, [roomQuery.data]);

  // 배치된 가구 → 서버 placementId. 포즈 저장에 쓴다. 낙관적 갱신 중에는 placementId가
  // 0(placeholder)일 수 있으나, 포즈는 서버 저장이 끝난 실제 배치에서만 유발된다.
  const placementIdByType = useMemo<Partial<Record<FurnitureType, number>>>(() => {
    const map: Partial<Record<FurnitureType, number>> = {};
    for (const placement of roomQuery.data?.placements ?? []) {
      const type = typeFromImageUrl(placement.imageUrl);
      if (type) {
        map[type] = placement.placementId;
      }
    }
    return map;
  }, [roomQuery.data]);

  // FurnitureType → 보유 userItemId. 배치 저장 시 서버가 userItemId를 요구한다.
  const typeToUserItemId = useMemo(() => {
    const map = new Map<FurnitureType, number>();
    for (const group of inventoryQuery.data?.groups ?? []) {
      for (const item of group.items) {
        const type = typeFromImageUrl(item.imageUrl);
        if (type) {
          map.set(type, item.userItemId);
        }
      }
    }
    return map;
  }, [inventoryQuery.data]);

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

      const next: Placements = { ...placements };
      if (position) {
        next[type] = clampPosition(type, position);
      } else {
        delete next[type];
      }

      // 저장 페이로드(전체 교체). 배치된 가구 중 하나라도 userItemId를 못 찾으면
      // 부분 목록으로 덮어써 삭제되는 사고를 막기 위해 저장·낙관적 갱신을 모두 건너뛴다.
      const payload: { userItemId: number; gridX: number; gridY: number }[] = [];
      for (const [placedType, placedPos] of Object.entries(next) as [FurnitureType, FurniturePosition][]) {
        const userItemId = typeToUserItemId.get(placedType);
        if (userItemId === undefined) {
          return true;
        }
        payload.push({ userItemId, gridX: placedPos.col, gridY: placedPos.row });
      }

      // 캐시를 낙관적으로 갱신해 화면 즉시 반영(placements가 여기서 파생된다).
      queryClient.setQueryData<RoomResponse>(ROOM_QUERY_KEY, (old) =>
        old
          ? {
              ...old,
              placements: (Object.entries(next) as [FurnitureType, FurniturePosition][]).map(([t, pos]) => ({
                placementId: 0,
                userItemId: typeToUserItemId.get(t) ?? 0,
                itemId: 0,
                itemName: "",
                imageUrl: `/assets/room/${FURNITURE_ITEM_STEM[t]}.png`,
                gridX: pos.col,
                gridY: pos.row,
                metadata: null,
              })),
            }
          : old,
      );

      savePlacements.mutate({ placements: payload });
      return true;
    },
    [placements, typeToUserItemId, queryClient, savePlacements],
  );

  const value = useMemo<RoomFurnitureContextValue>(
    () => ({ placements, placementIdByType, setPlacement }),
    [placements, placementIdByType, setPlacement],
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
