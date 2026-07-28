import { httpClient } from "../../shared/api/httpClient";
import type {
  ApiEnvelope,
  CompletedProblemSetDetail,
  CompletedProblemSetSummary,
} from "./types";

export type CompletedProblemSetFilters = {
  subjectId?: number;
  categoryLv1Id?: number;
  categoryLv2Id?: number;
};

/**
 * 완료한 문제집(모의고사/개념 문제집 등) 기록 조회 REST 호출. 회원 전용.
 */
export const completedProblemSetApi = {
  list(
    accessToken: string,
    filters: CompletedProblemSetFilters = {},
  ): Promise<CompletedProblemSetSummary[]> {
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
    const query = params.toString();

    return httpClient
      .get<ApiEnvelope<CompletedProblemSetSummary[]>>(
        `/api/v1/problem-sets/completed${query ? `?${query}` : ""}`,
        { accessToken },
      )
      .then((res) => res.data);
  },

  detail(
    solveSessionId: number,
    accessToken: string,
  ): Promise<CompletedProblemSetDetail> {
    return httpClient
      .get<ApiEnvelope<CompletedProblemSetDetail>>(
        `/api/v1/problem-sets/completed/${solveSessionId}`,
        { accessToken },
      )
      .then((res) => res.data);
  },
};
