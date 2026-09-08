import { httpClient } from "../../shared/api/httpClient";
import type {
  ApiEnvelope,
  CompleteSolveSessionResponse,
  GuestAnswerResult,
  QuestionHint,
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

  startSubjectMockSession(
    subjectId: number,
    accessToken: string,
  ): Promise<StartSolveSessionResponse> {
    return httpClient
      .post<ApiEnvelope<StartSolveSessionResponse>>(
        "/api/v1/solve-sessions/mocks/subject",
        { targetId: subjectId },
        { accessToken },
      )
      .then((res) => res.data);
  },

  startCategoryLv1MockSession(
    categoryLv1Id: number,
    accessToken: string,
  ): Promise<StartSolveSessionResponse> {
    return httpClient
      .post<ApiEnvelope<StartSolveSessionResponse>>(
        "/api/v1/solve-sessions/mocks/category-lv1",
        { targetId: categoryLv1Id },
        { accessToken },
      )
      .then((res) => res.data);
  },

  startCategoryLv2MockSession(
    categoryLv2Id: number,
    accessToken: string,
  ): Promise<StartSolveSessionResponse> {
    return httpClient
      .post<ApiEnvelope<StartSolveSessionResponse>>(
        "/api/v1/solve-sessions/mocks/category-lv2",
        { targetId: categoryLv2Id },
        { accessToken },
      )
      .then((res) => res.data);
  },

  submitAnswer(
    sessionId: number,
    body: {
      questionId: number;
      selectedAnswer: string;
      timeSpentSec: number;
      optionChangeCount: number;
      isAnswerChanged: boolean;
      hintUsed: boolean;
      hintCount: number;
    },
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

  /**
   * 회차 완료. XP·코인이 지급되는 요청이라 서버가 Idempotency-Key 헤더를 필수로 요구한다
   * (없으면 IDEMPOTENCY_KEY_REQUIRED 로 거절돼 회차를 끝낼 수 없다).
   *
   * 한 회차는 평생 한 번만 완료되므로 키를 sessionId 로 고정한다. 연타·재시도가 전부 같은
   * 키를 쓰게 돼 이중 지급이 원천적으로 막히고, 서버에서 실패해 롤백되면 키도 함께 지워져
   * 같은 키로 다시 시도할 수 있다.
   */
  completeSession(
    sessionId: number,
    totalTimeSpentSec: number,
    accessToken: string,
  ): Promise<CompleteSolveSessionResponse> {
    return httpClient
      .post<ApiEnvelope<CompleteSolveSessionResponse>>(
        `/api/v1/solve-sessions/${sessionId}/complete`,
        { totalTimeSpentSec },
        {
          accessToken,
          headers: { "Idempotency-Key": `solve-complete-${sessionId}` },
        },
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

  // 비회원/회원 공통으로 호출 가능(permitAll). 힌트가 없는 문제는 404.
  getHint(questionId: number): Promise<QuestionHint> {
    return httpClient
      .get<ApiEnvelope<QuestionHint>>(`/api/v1/solve/questions/${questionId}/hint`)
      .then((res) => res.data);
  },
};
