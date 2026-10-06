import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { AVATAR_WRITE, isLastAvatarWrite } from "./avatarWrites";
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
    ...AVATAR_WRITE,
    mutationFn: (body: SaveRoomPlacementsRequest) =>
      roomApi.savePlacements(body, accessToken as string),
    onSuccess: (room) => {
      // 서버가 저장된 최종 방 상태를 돌려주므로 캐시를 그대로 갱신한다.
      if (isLastAvatarWrite(queryClient)) {
        queryClient.setQueryData(["avatar", "room"], room);
      }
    },
    // 호출부가 낙관적으로 바꿔 둔 배치를 서버 값으로 되돌린다.
    onError: () => {
      queryClient.invalidateQueries({ queryKey: ["avatar", "room"] });
    },
  });
}

export function useSavePoseMutation(accessToken: string | null) {
  const queryClient = useQueryClient();

  return useMutation({
    ...AVATAR_WRITE,
    mutationFn: (body: SaveRoomPoseRequest) => roomApi.savePose(body, accessToken as string),
    onSuccess: (room) => {
      if (isLastAvatarWrite(queryClient)) {
        queryClient.setQueryData(["avatar", "room"], room);
      }
    },
    onError: () => {
      queryClient.invalidateQueries({ queryKey: ["avatar", "room"] });
    },
  });
}
