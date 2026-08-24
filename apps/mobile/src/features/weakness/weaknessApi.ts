import { httpClient } from "../../shared/api/httpClient";
import type {
  ApiEnvelope,
  CreateWeaknessAnalysisReportRequest,
  EligibleSolveSession,
  WeaknessAnalysisCost,
  WeaknessAnalysisReportDetail,
  WeaknessAnalysisReportSummary,
} from "./types";

export type EligibleSolveSessionFilters = {
  subjectId?: number;
  categoryLv1Id?: number;
  categoryLv2Id?: number;
  conceptId?: number;
};

/**
 * 사용자 약점 분석 REST 호출. 전부 회원 전용.
 */
export const weaknessApi = {
  getEligibleSessions(
    accessToken: string,
    filters: EligibleSolveSessionFilters = {},
  ): Promise<EligibleSolveSession[]> {
    const params = new URLSearchParams();
    if (filters.subjectId !== undefined) {
      params.set("subjectId", String(filters.subjectId));
    }
    if (filters.categoryLv1Id !== undefined) {
      params.set("categoryLv1Id", String(filters.categoryLv1Id));
    }
    if (filters.categoryLv2Id !== undefined) {
      params.set("categoryLv2Id", String(filters.categoryLv2Id));
    }
    if (filters.conceptId !== undefined) {
      params.set("conceptId", String(filters.conceptId));
    }
    const query = params.toString();

    return httpClient
      .get<ApiEnvelope<EligibleSolveSession[]>>(
        `/api/v1/weakness/eligible-solve-sessions${query ? `?${query}` : ""}`,
        { accessToken },
      )
      .then((res) => res.data);
  },

  // 생성 확인 화면(SCR-WEAK-03)의 "필요한 코인". 생성 요청과 같은 정책을 읽으므로 실제 차감액과 같다.
  getAnalysisCost(accessToken: string): Promise<WeaknessAnalysisCost> {
    return httpClient
      .get<ApiEnvelope<WeaknessAnalysisCost>>("/api/v1/weakness/analysis-cost", {
        accessToken,
      })
      .then((res) => res.data);
  },

  // 동기 호출: AI 해석까지 끝난 뒤 완성된 분석표를 응답으로 받는다(시간이 걸릴 수 있음).
  createReport(
    request: CreateWeaknessAnalysisReportRequest,
    accessToken: string,
  ): Promise<WeaknessAnalysisReportDetail> {
    return httpClient
      .post<ApiEnvelope<WeaknessAnalysisReportDetail>>(
        "/api/v1/weakness/reports",
        request,
        { accessToken },
      )
      .then((res) => res.data);
  },

  getReports(accessToken: string): Promise<WeaknessAnalysisReportSummary[]> {
    return httpClient
      .get<ApiEnvelope<WeaknessAnalysisReportSummary[]>>(
        "/api/v1/weakness/reports",
        { accessToken },
      )
      .then((res) => res.data);
  },

  getReportDetail(
    analysisReportId: number,
    accessToken: string,
  ): Promise<WeaknessAnalysisReportDetail> {
    return httpClient
      .get<ApiEnvelope<WeaknessAnalysisReportDetail>>(
        `/api/v1/weakness/reports/${analysisReportId}`,
        { accessToken },
      )
      .then((res) => res.data);
  },

  deleteReport(
    analysisReportId: number,
    accessToken: string,
  ): Promise<WeaknessAnalysisReportSummary> {
    return httpClient
      .delete<ApiEnvelope<WeaknessAnalysisReportSummary>>(
        `/api/v1/weakness/reports/${analysisReportId}`,
        { accessToken },
      )
      .then((res) => res.data);
  },
};
