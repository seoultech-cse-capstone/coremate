import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { useAuth } from "../auth/AuthContext";
import { deviceApi } from "./deviceApi";
import type { DeviceSession } from "./types";

const SESSIONS_QUERY_KEY = ["auth", "sessions"];

/** 로그인된 기기 목록. 마지막 접속이 최근인 순서로 서버가 정렬해 준다. */
export function useSessionsQuery() {
  const { accessToken } = useAuth();

  return useQuery<DeviceSession[]>({
    queryKey: SESSIONS_QUERY_KEY,
    queryFn: async () => {
      const res = await deviceApi.getSessions(accessToken as string);
      return res.sessions;
    },
    enabled: Boolean(accessToken),
  });
}

/** 특정 기기 로그아웃. 목록이 오래돼 이미 사라진 기기를 해지해도 서버가 성공으로 처리한다. */
export function useRevokeSessionMutation() {
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (sessionId: string) =>
      deviceApi.revokeSession(sessionId, accessToken as string),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: SESSIONS_QUERY_KEY }),
  });
}

/** 현재 기기를 제외한 모든 기기 로그아웃. */
export function useRevokeOtherSessionsMutation() {
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => deviceApi.revokeOtherSessions(accessToken as string),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: SESSIONS_QUERY_KEY }),
  });
}
