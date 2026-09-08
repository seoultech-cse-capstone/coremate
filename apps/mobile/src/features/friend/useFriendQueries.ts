import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { useIdempotencyKey } from "../../shared/hooks/useIdempotencyKey";
import { getApiErrorCode } from "../../shared/utils/apiError";
import { useAuth } from "../auth/AuthContext";
import { friendApi } from "./friendApi";
import type {
  Friend,
  FriendLearningHistory,
  FriendProfile,
  FriendRequest,
  FriendSearchUser,
} from "./types";

const FRIENDS_QUERY_KEY = ["friends"];
const FRIEND_REQUESTS_QUERY_KEY = ["friends", "requests"];

/** 콕 찌르기 한도 초과 시 서버가 돌려주는 에러 코드(EX-FRIEND-003). */
const POKE_LIMIT_CODES = [
  "FRIEND_POKE_FRIEND_LIMIT_EXCEEDED",
  "FRIEND_POKE_DAILY_LIMIT_EXCEEDED",
];

export function isPokeLimitError(error: unknown): boolean {
  const code = getApiErrorCode(error);
  return code !== null && POKE_LIMIT_CODES.includes(code);
}

/** 수락된 친구 목록. */
export function useFriendsQuery() {
  const { accessToken } = useAuth();

  return useQuery<Friend[]>({
    queryKey: FRIENDS_QUERY_KEY,
    queryFn: () => friendApi.getFriends(accessToken as string),
    enabled: Boolean(accessToken),
  });
}

/** 내가 받은 친구 요청. 보낸 요청은 화면에서 쓰지 않아 RECEIVED만 가져온다. */
export function useReceivedRequestsQuery() {
  const { accessToken } = useAuth();

  return useQuery<FriendRequest[]>({
    queryKey: FRIEND_REQUESTS_QUERY_KEY,
    queryFn: () => friendApi.getRequests("RECEIVED", accessToken as string),
    enabled: Boolean(accessToken),
  });
}

/**
 * 닉네임으로 사용자 검색. 검색어가 비어 있으면 호출하지 않는다.
 * 서버가 이미 친구이거나 요청 중인 상태를 함께 내려주므로 버튼 상태를 그대로 그릴 수 있다.
 */
export function useUserSearchQuery(keyword: string) {
  const { accessToken } = useAuth();
  const trimmed = keyword.trim();

  return useQuery<FriendSearchUser[]>({
    queryKey: ["friends", "search", trimmed],
    queryFn: () => friendApi.searchUsers(trimmed, accessToken as string),
    enabled: Boolean(accessToken) && trimmed.length > 0,
  });
}

export function useSendFriendRequestMutation() {
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (receiverId: number) =>
      friendApi.sendRequest(receiverId, accessToken as string),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["friends", "search"] });
      queryClient.invalidateQueries({ queryKey: FRIEND_REQUESTS_QUERY_KEY });
    },
  });
}

/** 요청 수락·거절. 수락하면 친구 목록도 바뀌므로 함께 새로 읽는다. */
export function useRespondRequestMutation() {
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ friendId, status }: { friendId: number; status: "ACCEPTED" | "REJECTED" }) =>
      friendApi.respondRequest(friendId, status, accessToken as string),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: FRIENDS_QUERY_KEY });
      queryClient.invalidateQueries({ queryKey: FRIEND_REQUESTS_QUERY_KEY });
    },
  });
}

export function useDeleteFriendMutation() {
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (friendUserId: number) =>
      friendApi.deleteFriend(friendUserId, accessToken as string),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: FRIENDS_QUERY_KEY }),
  });
}

/**
 * 콕 찌르기. 한도(같은 친구 1일 3회, 전체 1일 30회)를 넘기면 서버가 429로 막는다.
 * 성공한 대상은 화면에서 버튼을 눌린 상태로 유지하고, 한도 초과는 안내 문구로 보여준다.
 */
export function usePokeMutation() {
  const { accessToken } = useAuth();
  const idempotency = useIdempotencyKey();

  return useMutation({
    // 같은 친구를 연타하면 같은 키가 나가 알림이 두 번 가지 않는다. 성공 후에는 키를
    // 비워, 하루 3회까지 허용된 다음 찌르기를 막지 않는다.
    mutationFn: (toUserId: number) =>
      friendApi.poke(toUserId, accessToken as string, idempotency.keyFor(`poke-${toUserId}`)),
    onSuccess: () => idempotency.clear(),
  });
}

/** 친구 상세 프로필. 과목별 스탯이 함께 온다. */
export function useFriendProfileQuery(friendUserId: number | null) {
  const { accessToken } = useAuth();

  return useQuery<FriendProfile>({
    queryKey: ["friends", "profile", friendUserId],
    queryFn: () => friendApi.getProfile(friendUserId as number, accessToken as string),
    enabled: Boolean(accessToken) && friendUserId !== null,
  });
}

export function useFriendLearningHistoryQuery(friendUserId: number | null) {
  const { accessToken } = useAuth();

  return useQuery<FriendLearningHistory>({
    queryKey: ["friends", "learning-history", friendUserId],
    queryFn: () => friendApi.getLearningHistory(friendUserId as number, accessToken as string),
    enabled: Boolean(accessToken) && friendUserId !== null,
  });
}
