import { httpClient } from "../../shared/api/httpClient";
import type {
  ApiEnvelope,
  DailyQuizAnswerInput,
  DailyQuizResultItem,
  DailyQuizSet,
  DailyQuizSubmitResult,
} from "./types";

/**
 * 오늘의 CS 퀴즈 REST 호출. 조회/제출/보상 조회는 실제로 머지된 API이고,
 * getResult()는 백엔드 PR #86(미머지)의 계약을 미리 맞춰둔 것이라
 * 머지 전까지는 실패한다.
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

  // TODO(#102 후속): 백엔드 PR #86 머지되면 실제 응답 형태 확인 후 필요시 교체.
  getResult(accessToken: string): Promise<DailyQuizResultItem[]> {
    return httpClient
      .get<ApiEnvelope<DailyQuizResultItem[]>>("/api/v1/daily-quizzes/result", {
        accessToken,
      })
      .then((res) => res.data);
  },
};
