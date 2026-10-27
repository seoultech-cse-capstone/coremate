import { httpClient } from "../../shared/api/httpClient";

export type InquiryCategory =
  | "HOME"
  | "FRIEND"
  | "PROBLEM_SET"
  | "REVIEW"
  | "MYPAGE";

type ApiResponse<T> = { success: boolean; message: string; data: T };

/**
 * 고객지원(문의하기·버그 신고) REST 호출.
 */
export const supportApi = {
  createInquiry(
    input: { category: InquiryCategory; title: string; body: string },
    accessToken: string,
  ): Promise<ApiResponse<unknown>> {
    return httpClient.post<ApiResponse<unknown>>(
      "/api/v1/support/inquiries",
      input,
      { accessToken },
    );
  },

  createBugReport(
    body: string,
    accessToken: string,
  ): Promise<ApiResponse<unknown>> {
    return httpClient.post<ApiResponse<unknown>>(
      "/api/v1/support/bug-reports",
      { body },
      { accessToken },
    );
  },
};
