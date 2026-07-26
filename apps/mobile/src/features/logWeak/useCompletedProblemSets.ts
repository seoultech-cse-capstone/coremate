import { useQuery } from "@tanstack/react-query";

import {
  completedProblemSetApi,
  type CompletedProblemSetFilters,
} from "./completedProblemSetApi";

export function useCompletedProblemSets(
  accessToken: string | null,
  filters: CompletedProblemSetFilters,
) {
  return useQuery({
    queryKey: [
      "log-weak",
      "completed-problem-sets",
      filters.subjectId ?? null,
      filters.categoryLv1Id ?? null,
      filters.categoryLv2Id ?? null,
    ],
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
