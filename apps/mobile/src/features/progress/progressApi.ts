import { httpClient } from "../../shared/api/httpClient";
import type {
  ApiEnvelope,
  ConceptCountSummary,
  ProblemSetPassRate,
  ProblemSetProgress,
  ProblemSetProgressFilters,
  ProgressScopeType,
  SubjectProgress,
} from "./types";

/**
 * 학습 진행률 REST 호출. 세 엔드포인트 모두 백엔드가 실제 DB 조회로 응답한다.
 *
 * userId 쿼리 파라미터는 백엔드가 @AuthenticationPrincipal 로 바뀌면서 무시된다.
 * 호출 자체는 정상 동작하므로 남겨 두었다.
 */
export const progressApi = {
  getConceptCountSummary(accessToken: string): Promise<ConceptCountSummary> {
    return httpClient
      .get<ApiEnvelope<ConceptCountSummary>>("/api/v1/progress/concepts/summary", {
        accessToken,
      })
      .then((res) => res.data);
  },

  getSubjectProgress(accessToken: string, userId: number): Promise<SubjectProgress[]> {
    return httpClient
      .get<ApiEnvelope<SubjectProgress[]>>(`/api/v1/progress/subjects?userId=${userId}`, {
        accessToken,
      })
      .then((res) => res.data);
  },

  getPassRates(
    accessToken: string,
    scopeType: ProgressScopeType,
    parentId: number | null,
  ): Promise<ProblemSetPassRate[]> {
    const params = new URLSearchParams({ scopeType });
    if (parentId !== null) {
      params.set("parentId", String(parentId));
    }

    return httpClient
      .get<ApiEnvelope<ProblemSetPassRate[]>>(`/api/v1/progress/pass-rates?${params}`, {
        accessToken,
      })
      .then((res) => res.data);
  },

  getProblemSetProgress(
    accessToken: string,
    userId: number,
    filters: ProblemSetProgressFilters = {},
  ): Promise<ProblemSetProgress[]> {
    const params = new URLSearchParams({ userId: String(userId) });
    if (filters.problemSetType !== undefined) {
      params.set("problemSetType", filters.problemSetType);
    }
    if (filters.subjectId !== undefined) {
      params.set("subjectId", String(filters.subjectId));
    }
    if (filters.categoryLv1Id !== undefined) {
      params.set("categoryLv1Id", String(filters.categoryLv1Id));
    }
    if (filters.categoryLv2Id !== undefined) {
      params.set("categoryLv2Id", String(filters.categoryLv2Id));
    }

    return httpClient
      .get<ApiEnvelope<ProblemSetProgress[]>>(`/api/v1/progress/problem-sets?${params}`, {
        accessToken,
      })
      .then((res) => res.data);
  },
};
