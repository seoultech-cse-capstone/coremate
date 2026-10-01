import { httpClient } from "../../shared/api/httpClient";
import type {
  BlockedUser,
  Friend,
  FriendLearningHistory,
  FriendPoke,
  FriendProfile,
  FriendRequest,
  FriendSearchUser,
} from "./types";

/**
 * 친구 REST 호출. 전부 회원 전용이다.
 *
 * accessToken을 인자로 받는 것은 다른 도메인 API 모듈과 같은 컨벤션이다.
 * (features/auth를 import하지 않으므로 모듈 순환이 생기지 않는다)
 */
export const friendApi = {
  getFriends(accessToken: string): Promise<Friend[]> {
    return httpClient.get<Friend[]>("/api/friends", { accessToken });
  },

  getRequests(direction: "RECEIVED" | "SENT" | "ALL", accessToken: string): Promise<FriendRequest[]> {
    return httpClient.get<FriendRequest[]>(
      `/api/friends/requests?direction=${direction}`,
      { accessToken },
    );
  },

  searchUsers(keyword: string, accessToken: string): Promise<FriendSearchUser[]> {
    return httpClient.get<FriendSearchUser[]>(
      `/api/friends/search?keyword=${encodeURIComponent(keyword)}`,
      { accessToken },
    );
  },

  sendRequest(receiverId: number, accessToken: string): Promise<FriendRequest> {
    return httpClient.post<FriendRequest>("/api/friends/requests", { receiverId }, { accessToken });
  },

  respondRequest(
    friendId: number,
    status: "ACCEPTED" | "REJECTED",
    accessToken: string,
  ): Promise<Friend> {
    return httpClient.post<Friend>(
      "/api/friends/requests/respond",
      { friendId, status },
      { accessToken },
    );
  },

  deleteFriend(friendUserId: number, accessToken: string): Promise<void> {
    return httpClient.delete<void>(`/api/friends/${friendUserId}`, { accessToken });
  },

  // 차단하면 친구였어도 관계가 끊기고, 상대는 나를 검색하거나 친구 요청을 보낼 수 없다.
  block(targetUserId: number, accessToken: string): Promise<void> {
    return httpClient.post<void>(`/api/friends/${targetUserId}/block`, undefined, { accessToken });
  },

  unblock(targetUserId: number, accessToken: string): Promise<void> {
    return httpClient.delete<void>(`/api/friends/${targetUserId}/block`, { accessToken });
  },

  getBlockedUsers(accessToken: string): Promise<BlockedUser[]> {
    return httpClient.get<BlockedUser[]>("/api/friends/blocks", { accessToken });
  },

  getProfile(friendUserId: number, accessToken: string): Promise<FriendProfile> {
    return httpClient.get<FriendProfile>(`/api/friends/${friendUserId}/profile`, { accessToken });
  },

  getLearningHistory(friendUserId: number, accessToken: string): Promise<FriendLearningHistory> {
    return httpClient.get<FriendLearningHistory>(
      `/api/friends/${friendUserId}/learning-history`,
      { accessToken },
    );
  },

  // 알림이 나가는 요청이라 서버가 Idempotency-Key 를 필수로 요구한다(연타 = 알림 폭탄).
  // 같은 친구를 하루 3회까지 찌를 수 있으므로 키를 고정하지 않고 호출부가 수명을 정한다.
  poke(
    toUserId: number,
    accessToken: string,
    idempotencyKey: string,
  ): Promise<FriendPoke> {
    return httpClient.post<FriendPoke>(
      "/api/friends/pokes",
      { toUserId, messageType: "POKE" },
      { accessToken, headers: { "Idempotency-Key": idempotencyKey } },
    );
  },
};
