import type { AvatarEquippedItem, RoomPose } from "./types";

/** 서버 GET /api/avatar/room 의 배치 한 개. (com.coremate.backend RoomPlacementResponse) */
export type RoomPlacement = {
  placementId: number;
  userItemId: number;
  itemId: number;
  itemName: string;
  imageUrl: string | null;
  /** 격자 x 좌표(0부터, = col). 여러 칸 가구는 왼쪽 윗칸 */
  gridX: number;
  /** 격자 y 좌표(0부터, = row) */
  gridY: number;
  metadata: unknown;
};

/** 서버 GET /api/avatar/room 응답. (RoomResponse) */
export type RoomResponse = {
  userId: number;
  baseAvatarType: string;
  gridWidth: number;
  gridHeight: number;
  equippedItems: AvatarEquippedItem[];
  placements: RoomPlacement[];
  roomPose: RoomPose | null;
  roomPosePlacementId: number | null;
  roomPoseUpdatedAt: string | null;
  roomPoseElapsedText: string | null;
};

/** 서버 PUT /api/avatar/room/placements 요청. 배치 전체를 통째로 교체한다. */
export type SaveRoomPlacementsRequest = {
  placements: {
    userItemId: number;
    gridX: number;
    gridY: number;
  }[];
};
