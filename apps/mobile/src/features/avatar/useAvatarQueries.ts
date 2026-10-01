import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { avatarApi } from "./avatarApi";
import type { UpdateEquippedItemsRequest } from "./types";

export function useMyAvatar(accessToken: string | null) {
  return useQuery({
    queryKey: ["avatar", "me"],
    queryFn: () => avatarApi.getMyAvatar(accessToken as string),
    enabled: accessToken !== null,
  });
}

export function useUpdateEquippedItemsMutation(accessToken: string | null) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: UpdateEquippedItemsRequest) =>
      avatarApi.updateEquippedItems(body, accessToken as string),
    onSuccess: (profile) => {
      // 장착 변경은 보관함의 equipped 표시에도 영향을 준다.
      queryClient.setQueryData(["avatar", "me"], (current: unknown) => {
        if (!current || typeof current !== "object") {
          return current;
        }
        return { ...current, profile };
      });
      queryClient.invalidateQueries({ queryKey: ["avatar", "inventory"] });
      // WALLPAPER(테마) 등 장착 변경은 방 조회의 equippedItems에도 반영돼야 한다.
      queryClient.invalidateQueries({ queryKey: ["avatar", "room"] });
    },
    // 호출부가 낙관적으로 바꿔 둔 장착(내 아바타·방 테마)을 서버 값으로 되돌린다.
    // 안 그러면 다음 조회 전까지 나는 바뀐 모습을 보고 친구는 예전 모습을 본다.
    onError: () => {
      queryClient.invalidateQueries({ queryKey: ["avatar", "me"] });
      queryClient.invalidateQueries({ queryKey: ["avatar", "room"] });
    },
  });
}
