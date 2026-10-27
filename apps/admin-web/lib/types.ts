export type InquiryStatus = "PENDING" | "ANSWERED";

export type InquiryCategory =
  | "HOME"
  | "FRIEND"
  | "PROBLEM_SET"
  | "REVIEW"
  | "MYPAGE";

export type BugReportStatus = "RECEIVED" | "RESOLVED";

export interface Inquiry {
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
}

export interface BugReport {
  bugReportId: number;
  userId: number;
  nickname: string;
  body: string;
  status: BugReportStatus;
  createdAt: string;
}

export const CATEGORY_LABEL: Record<InquiryCategory, string> = {
  HOME: "홈",
  FRIEND: "친구",
  PROBLEM_SET: "문제집",
  REVIEW: "복습",
  MYPAGE: "마이페이지",
};
