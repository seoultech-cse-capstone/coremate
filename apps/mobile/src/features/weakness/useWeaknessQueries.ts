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

export function useWeaknessAnalysisCost(accessToken: string | null) {
  return useQuery({
    queryKey: ["weakness", "analysis-cost"],
    queryFn: () => weaknessApi.getAnalysisCost(accessToken as string),
    enabled: accessToken !== null,
  });
}

// 생성 중(GENERATING) 분석표가 완료되는지 확인하는 폴링 간격(ms). FCM 을 못 받는 상황
// (알림 권한 꺼짐·포그라운드 등) 대비 fallback 이다. 완료/실패로 바뀌면 폴링을 멈춘다.
const GENERATING_POLL_MS = 3000;

export function useWeaknessReports(accessToken: string | null) {
  return useQuery({
    queryKey: ["weakness", "reports"],
    queryFn: () => weaknessApi.getReports(accessToken as string),
    enabled: accessToken !== null,
    // 목록에 생성 중 분석표가 하나라도 있으면 완료될 때까지 주기적으로 다시 읽는다.
    refetchInterval: (query) =>
      (query.state.data ?? []).some((report) => report.status === "GENERATING")
        ? GENERATING_POLL_MS
        : false,
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
    // 생성 중인 분석표를 보고 있으면 완료될 때까지 폴링한다.
    refetchInterval: (query) =>
      query.state.data?.status === "GENERATING" ? GENERATING_POLL_MS : false,
  });
}
