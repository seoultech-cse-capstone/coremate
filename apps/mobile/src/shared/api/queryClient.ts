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
