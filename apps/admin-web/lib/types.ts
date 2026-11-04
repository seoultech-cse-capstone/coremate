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

export type UserStatus = "ACTIVE" | "WITHDRAWN" | "BANNED";

export type SocialProvider = "GOOGLE" | "KAKAO" | "NAVER" | "APPLE";

export interface AdminUser {
  userId: number;
  email: string | null;
  nickname: string;
  status: UserStatus;
  socialProviders: SocialProvider[];
  createdAt: string;
  lastLoginAt: string | null;
}

export const USER_STATUS_LABEL: Record<UserStatus, string> = {
  ACTIVE: "이용 중",
  WITHDRAWN: "탈퇴함",
  BANNED: "이용 정지",
};

export const PROVIDER_LABEL: Record<SocialProvider, string> = {
  GOOGLE: "구글",
  KAKAO: "카카오",
  NAVER: "네이버",
  APPLE: "Apple",
};

export const CATEGORY_LABEL: Record<InquiryCategory, string> = {
  HOME: "홈",
  FRIEND: "친구",
  PROBLEM_SET: "문제집",
  REVIEW: "복습",
  MYPAGE: "마이페이지",
};
