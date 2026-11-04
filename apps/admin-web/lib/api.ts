import type {
  AdminUser,
  BugReport,
  BugReportStatus,
  Inquiry,
  InquiryStatus,
} from "./types";

const BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? "";
const TOKEN_KEY = "coremate_admin_token";

// 백엔드 공통 응답 래퍼.
interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  window.localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  window.localStorage.removeItem(TOKEN_KEY);
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const token = getToken();
  const res = await fetch(`${BASE}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init?.headers,
    },
  });

  // 토큰 만료·권한 없음이면 로그인으로 돌려보낸다.
  if (res.status === 401 || res.status === 403) {
    clearToken();
    if (typeof window !== "undefined") {
      window.location.href = "/login";
    }
    throw new Error("인증이 필요합니다. 다시 로그인해 주세요.");
  }

  const data = res.status === 204 ? null : await res.json();
  if (!res.ok) {
    throw new Error((data as { message?: string })?.message ?? "요청에 실패했습니다.");
  }
  return data as T;
}

// 로그인은 ApiResponse 래퍼 없이 토큰을 직접 반환한다.
export async function login(email: string, password: string): Promise<void> {
  const res = await fetch(`${BASE}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });
  const data = await res.json();
  if (!res.ok) {
    throw new Error(data?.message ?? "로그인에 실패했습니다.");
  }
  setToken(data.accessToken as string);
}

export async function listInquiries(status?: InquiryStatus): Promise<Inquiry[]> {
  const query = status ? `?status=${status}` : "";
  const res = await request<ApiResponse<Inquiry[]>>(
    `/api/v1/admin/support/inquiries${query}`,
  );
  return res.data;
}

export async function answerInquiry(
  inquiryId: number,
  answer: string,
): Promise<Inquiry> {
  const res = await request<ApiResponse<Inquiry>>(
    `/api/v1/admin/support/inquiries/${inquiryId}/answer`,
    { method: "PATCH", body: JSON.stringify({ answer }) },
  );
  return res.data;
}

export async function listBugReports(
  status?: BugReportStatus,
): Promise<BugReport[]> {
  const query = status ? `?status=${status}` : "";
  const res = await request<ApiResponse<BugReport[]>>(
    `/api/v1/admin/support/bug-reports${query}`,
  );
  return res.data;
}

export async function updateBugReportStatus(
  bugReportId: number,
  status: BugReportStatus,
): Promise<BugReport> {
  const res = await request<ApiResponse<BugReport>>(
    `/api/v1/admin/support/bug-reports/${bugReportId}/status?status=${status}`,
    { method: "PATCH" },
  );
  return res.data;
}

// 메일로 온 계정 삭제 요청 처리용. 가입 이메일이나 닉네임이 정확히 같은 회원을 찾는다.
export async function searchUsers(keyword: string): Promise<AdminUser[]> {
  const res = await request<ApiResponse<AdminUser[]>>(
    `/api/v1/admin/users?keyword=${encodeURIComponent(keyword)}`,
  );
  return res.data;
}

export async function withdrawUser(userId: number): Promise<void> {
  await request<ApiResponse<null>>(`/api/v1/admin/users/${userId}/withdraw`, {
    method: "POST",
  });
}
