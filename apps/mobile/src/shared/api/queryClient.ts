import { QueryClient } from "@tanstack/react-query";

import { HttpError } from "./httpClient";

/**
 * 앱 전역에서 공유하는 TanStack Query 클라이언트.
 * - 4xx 응답은 재시도하지 않는다(잘못된 요청/인증 실패는 재시도해도 동일).
 * - 그 외 오류(네트워크 등)만 최대 2회 재시도한다.
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: (failureCount, error) => {
        if (error instanceof HttpError && error.status >= 400 && error.status < 500) {
          return false;
        }
        return failureCount < 2;
      },
    },
    mutations: {
      retry: false,
    },
  },
});

/**
 * 화면에 떠 있는 쿼리를 다시 불러온다.
 * 탭 화면은 한 번 마운트되면 계속 살아 있어 refetchOnMount 가 걸리지 않으므로,
 * 탭을 누르는 시점에 이걸 불러 서버에서 바뀐 내용을 가져온다.
 * 떠 있지 않은 쿼리는 stale 로만 표시돼 다음에 마운트될 때 다시 불러온다.
 */
export function refetchActiveQueries(): Promise<void> {
  return queryClient.invalidateQueries({ refetchType: "active" });
}
