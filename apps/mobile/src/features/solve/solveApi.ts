import { httpClient } from "../../shared/api/httpClient";
import type {
  ApiEnvelope,
  CompleteSolveSessionResponse,
  GuestAnswerResult,
  StartSolveSessionResponse,
  SubmitAnswerResponse,
} from "./types";

/**
 * 개념 문제집(CONCEPT_PRACTICE) 풀이 세션 REST 호출.
 * /concept, /guest/complete는 permitAll(게스트 가능), 나머지는 회원 전용.
 */
export const solveApi = {
  startConceptSession(
    conceptId: number,
    accessToken?: string,
  ): Promise<StartSolveSessionResponse> {
    return httpClient
      .post<ApiEnvelope<StartSolveSessionResponse>>(
        "/api/v1/solve-sessions/concept",
        { sessionType: "CONCEPT_PRACTICE", targetId: conceptId },
        accessToken ? { accessToken } : undefined,
      )
      .then((res) => res.data);
  },

  submitAnswer(
    sessionId: number,
    body: { questionId: number; selectedAnswer: string; timeSpentSec: number },
    accessToken: string,
  ): Promise<SubmitAnswerResponse> {
    return httpClient
      .post<ApiEnvelope<SubmitAnswerResponse>>(
        `/api/v1/solve-sessions/${sessionId}/answers`,
        body,
        { accessToken },
      )
      .then((res) => res.data);
  },

  completeSession(
    sessionId: number,
    totalTimeSpentSec: number,
    accessToken: string,
  ): Promise<CompleteSolveSessionResponse> {
    return httpClient
      .post<ApiEnvelope<CompleteSolveSessionResponse>>(
        `/api/v1/solve-sessions/${sessionId}/complete`,
        { totalTimeSpentSec },
        { accessToken },
      )
      .then((res) => res.data);
  },

  completeGuestSession(
    conceptId: number,
    answers: { questionId: number; selectedAnswer: string }[],
  ): Promise<GuestAnswerResult[]> {
    return httpClient
      .post<ApiEnvelope<GuestAnswerResult[]>>(
        "/api/v1/solve-sessions/guest/complete",
        { conceptId, answers },
      )
      .then((res) => res.data);
  },
};
