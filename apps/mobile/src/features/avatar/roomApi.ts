import { httpClient } from "../../shared/api/httpClient";
import type { RoomResponse, SaveRoomPlacementsRequest } from "./roomTypes";

/** 방 도메인 REST 호출. AvatarController와 동일하게 ApiResponse 래핑 없이 DTO를 그대로 응답한다. */
export const roomApi = {
  getMyRoom(accessToken: string): Promise<RoomResponse> {
    return httpClient.get<RoomResponse>("/api/avatar/room", { accessToken });
  },

  savePlacements(
    body: SaveRoomPlacementsRequest,
    accessToken: string,
  ): Promise<RoomResponse> {
    return httpClient.put<RoomResponse>("/api/avatar/room/placements", body, {
      accessToken,
    });
  },
};
