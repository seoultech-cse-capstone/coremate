import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { roomApi } from "./roomApi";
import type { SaveRoomPlacementsRequest, SaveRoomPoseRequest } from "./roomTypes";

export function useMyRoom(accessToken: string | null) {
  return useQuery({
    queryKey: ["avatar", "room"],
    queryFn: () => roomApi.getMyRoom(accessToken as string),
    enabled: accessToken !== null,
  });
}

export function useSavePlacementsMutation(accessToken: string | null) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: SaveRoomPlacementsRequest) =>
      roomApi.savePlacements(body, accessToken as string),
    onSuccess: (room) => {
      // 서버가 저장된 최종 방 상태를 돌려주므로 캐시를 그대로 갱신한다.
      queryClient.setQueryData(["avatar", "room"], room);
    },
  });
}

export function useSavePoseMutation(accessToken: string | null) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: SaveRoomPoseRequest) => roomApi.savePose(body, accessToken as string),
    onSuccess: (room) => {
      queryClient.setQueryData(["avatar", "room"], room);
    },
  });
}
