import { httpClient } from "../../shared/api/httpClient";
import type {
  ApiEnvelope,
  DailyQuizAnswerInput,
  DailyQuizResult,
  DailyQuizSet,
  DailyQuizSubmitResult,
} from "./types";

/**
 * 오늘의 CS 퀴즈 REST 호출.
 */
export const dailyQuizApi = {
  getToday(accessToken: string): Promise<DailyQuizSet> {
    return httpClient
      .get<ApiEnvelope<DailyQuizSet>>("/api/v1/daily-quizzes/today", {
        accessToken,
      })
      .then((res) => res.data);
  },

  submit(
    dailyQuizSetId: number,
    answers: DailyQuizAnswerInput[],
    totalTimeSpentSec: number,
    accessToken: string,
  ): Promise<DailyQuizSubmitResult> {
    return httpClient
      .post<ApiEnvelope<DailyQuizSubmitResult>>(
        `/api/v1/daily-quizzes/${dailyQuizSetId}/submit`,
        { answers, totalTimeSpentSec },
        { accessToken },
      )
      .then((res) => res.data);
  },

  getResult(accessToken: string): Promise<DailyQuizResult> {
    return httpClient
      .get<ApiEnvelope<DailyQuizResult>>("/api/v1/daily-quizzes/result", {
        accessToken,
      })
      .then((res) => res.data);
  },
};
