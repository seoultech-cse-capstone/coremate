import { useQuery } from "@tanstack/react-query";

import {
  weaknessApi,
  type EligibleSolveSessionFilters,
} from "./weaknessApi";

export function useEligibleSolveSessions(
  accessToken: string | null,
  filters: EligibleSolveSessionFilters,
  enabled: boolean,
) {
  return useQuery({
    queryKey: [
      "weakness",
      "eligible-solve-sessions",
      filters.subjectId ?? null,
      filters.categoryLv1Id ?? null,
      filters.categoryLv2Id ?? null,
      filters.conceptId ?? null,
    ],
    queryFn: () => weaknessApi.getEligibleSessions(accessToken as string, filters),
    enabled: enabled && accessToken !== null,
  });
}

export function useWeaknessReports(accessToken: string | null) {
  return useQuery({
    queryKey: ["weakness", "reports"],
    queryFn: () => weaknessApi.getReports(accessToken as string),
    enabled: accessToken !== null,
  });
}

export function useWeaknessReportDetail(
  analysisReportId: number | null,
  accessToken: string | null,
) {
  return useQuery({
    queryKey: ["weakness", "report-detail", analysisReportId],
    queryFn: () =>
      weaknessApi.getReportDetail(analysisReportId as number, accessToken as string),
    enabled: analysisReportId !== null && accessToken !== null,
  });
}
