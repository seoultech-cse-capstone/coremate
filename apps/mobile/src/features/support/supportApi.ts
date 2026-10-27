import { httpClient } from "../../shared/api/httpClient";

export type InquiryCategory =
  | "HOME"
  | "FRIEND"
  | "PROBLEM_SET"
  | "REVIEW"
  | "MYPAGE";

export type InquiryStatus = "PENDING" | "ANSWERED";

/** GET /api/v1/support/inquiries 항목. 백엔드 InquiryResponse와 1:1 대응. */
export type Inquiry = {
  inquiryId: number;
  userId: number;
  nickname: string;
  category: InquiryCategory;
  title: string;
  body: string;
  status: InquiryStatus;
  answer: string | null;
  answeredAt: string | null;
  createdAt: string;
};

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

  async getMyInquiries(accessToken: string): Promise<Inquiry[]> {
    const response = await httpClient.get<ApiResponse<Inquiry[]>>(
      "/api/v1/support/inquiries",
      { accessToken },
    );
    return response.data;
  },

  async getMyInquiry(
    inquiryId: number,
    accessToken: string,
  ): Promise<Inquiry> {
    const response = await httpClient.get<ApiResponse<Inquiry>>(
      `/api/v1/support/inquiries/${inquiryId}`,
      { accessToken },
    );
    return response.data;
  },
};
