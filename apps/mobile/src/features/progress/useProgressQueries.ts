import { useQuery } from "@tanstack/react-query";

import { progressApi } from "./progressApi";
import type { ProblemSetProgressFilters, ProgressScopeType } from "./types";

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

/**
 * 문제집 목록의 진행바 값. 화면 깊이별로 범위를 지정해 부른다.
 *
 * 하위 개념 목록에는 쓰지 않는다 — 개념의 진행률은 개념 목록 API가 주는
 * 총 문제 수/정답 수로 계산하기 때문이다.
 */
export function usePassRates(
  accessToken: string | null,
  scopeType: ProgressScopeType | null,
  parentId: number | null,
) {
  return useQuery({
    queryKey: ["progress", "pass-rates", scopeType, parentId],
    queryFn: () =>
      progressApi.getPassRates(accessToken as string, scopeType as ProgressScopeType, parentId),
    enabled: accessToken !== null && scopeType !== null,
  });
}
