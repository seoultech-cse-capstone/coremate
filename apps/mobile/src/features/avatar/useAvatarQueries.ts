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
  });
}
