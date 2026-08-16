import { httpClient } from "../../shared/api/httpClient";
import type {
  ApiEnvelope,
  ConceptCountSummary,
  ConceptProgress,
  ProblemSetProgress,
  ProblemSetProgressFilters,
  SubjectProgress,
} from "./types";

/**
 * 학습 진행률 REST 호출.
 *
 * subjects/problem-sets/concepts 3개는 백엔드가 아직 DB 연동 전 더미 데이터를
 * 반환한다(ProgressController 주석 확인, 2026-08). 실제 값으로 바뀌면 이 파일
 * 수정 없이 그대로 동작한다.
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

  getConceptProgress(
    accessToken: string,
    userId: number,
    categoryLv2Id: number,
  ): Promise<ConceptProgress[]> {
    return httpClient
      .get<ApiEnvelope<ConceptProgress[]>>(
        `/api/v1/progress/concepts?userId=${userId}&categoryLv2Id=${categoryLv2Id}`,
        { accessToken },
      )
      .then((res) => res.data);
  },
};
