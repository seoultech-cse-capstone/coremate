import { useQuery } from "@tanstack/react-query";

import { progressApi } from "./progressApi";
import type { ProblemSetProgressFilters } from "./types";

export function useConceptCountSummary(accessToken: string | null) {
  return useQuery({
    queryKey: ["progress", "concept-count-summary"],
    queryFn: () => progressApi.getConceptCountSummary(accessToken as string),
    enabled: accessToken !== null,
  });
}

export function useSubjectProgress(accessToken: string | null, userId: number | null) {
  return useQuery({
    queryKey: ["progress", "subjects", userId],
    queryFn: () => progressApi.getSubjectProgress(accessToken as string, userId as number),
    enabled: accessToken !== null && userId !== null,
  });
}

export function useProblemSetProgress(
  accessToken: string | null,
  userId: number | null,
  filters: ProblemSetProgressFilters = {},
) {
  return useQuery({
    queryKey: [
      "progress",
      "problem-sets",
      userId,
      filters.problemSetType ?? null,
      filters.subjectId ?? null,
      filters.categoryLv1Id ?? null,
      filters.categoryLv2Id ?? null,
    ],
    queryFn: () =>
      progressApi.getProblemSetProgress(accessToken as string, userId as number, filters),
    enabled: accessToken !== null && userId !== null,
  });
}

export function useConceptProgress(
  accessToken: string | null,
  userId: number | null,
  categoryLv2Id: number | null,
) {
  return useQuery({
    queryKey: ["progress", "concepts", userId, categoryLv2Id],
    queryFn: () =>
      progressApi.getConceptProgress(
        accessToken as string,
        userId as number,
        categoryLv2Id as number,
      ),
    enabled: accessToken !== null && userId !== null && categoryLv2Id !== null,
  });
}
