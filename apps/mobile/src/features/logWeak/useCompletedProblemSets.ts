import { useQuery } from "@tanstack/react-query";

import {
  completedProblemSetApi,
  type CompletedProblemSetFilters,
} from "./completedProblemSetApi";

/** 문제집 탭의 "최근 풀이 및 해설"도 같은 캐시를 쓰도록 키를 공유한다. */
export function completedProblemSetsQueryKey(filters: CompletedProblemSetFilters) {
  return [
    "log-weak",
    "completed-problem-sets",
    filters.subjectId ?? null,
    filters.categoryLv1Id ?? null,
    filters.categoryLv2Id ?? null,
  ];
}

export function useCompletedProblemSets(
  accessToken: string | null,
  filters: CompletedProblemSetFilters,
) {
  return useQuery({
    queryKey: completedProblemSetsQueryKey(filters),
    queryFn: () => completedProblemSetApi.list(accessToken as string, filters),
    enabled: accessToken !== null,
  });
}

export function useCompletedProblemSetDetail(
  solveSessionId: number | null,
  accessToken: string | null,
) {
  return useQuery({
    queryKey: ["log-weak", "completed-problem-set-detail", solveSessionId],
    queryFn: () =>
      completedProblemSetApi.detail(solveSessionId as number, accessToken as string),
    enabled: solveSessionId !== null && accessToken !== null,
  });
}
