import { httpClient } from "../../shared/api/httpClient";
import type { AvatarProfile, UpdateEquippedItemsRequest, UserAvatar } from "./types";

/**
 * 아바타 도메인 REST 호출. AvatarController는 ApiResponse 래핑 없이 DTO를 그대로 응답한다.
 */
export const avatarApi = {
  getMyAvatar(accessToken: string): Promise<UserAvatar> {
    return httpClient.get<UserAvatar>("/api/avatar/me", { accessToken });
  },

  updateEquippedItems(
    body: UpdateEquippedItemsRequest,
    accessToken: string,
  ): Promise<AvatarProfile> {
    return httpClient.put<AvatarProfile>("/api/avatar/equipped-items", body, {
      accessToken,
    });
  },
};
