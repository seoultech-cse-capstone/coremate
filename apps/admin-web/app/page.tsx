"use client";

import { useRouter } from "next/navigation";
import { type FormEvent, useCallback, useEffect, useState } from "react";

import {
  answerInquiry,
  clearToken,
  getToken,
  listBugReports,
  listInquiries,
  searchUsers,
  updateBugReportStatus,
  withdrawUser,
} from "@/lib/api";
import {
  CATEGORY_LABEL,
  PROVIDER_LABEL,
  USER_STATUS_LABEL,
  type AdminUser,
  type BugReport,
  type Inquiry,
  type InquiryStatus,
} from "@/lib/types";

type Tab = "inquiries" | "bugs" | "users";

function formatDate(iso: string): string {
  return iso.replace("T", " ").slice(0, 16);
}

export default function DashboardPage() {
  const router = useRouter();
  const [ready, setReady] = useState(false);
  const [tab, setTab] = useState<Tab>("inquiries");

  useEffect(() => {
    if (!getToken()) {
      router.replace("/login");
      return;
    }
    setReady(true);
  }, [router]);

  if (!ready) {
    return null;
  }

  return (
    <div className="container">
      <div className="topbar">
        <h1>CoreMate 관리자</h1>
        <button
          className="btn ghost small"
          onClick={() => {
            clearToken();
            router.replace("/login");
          }}
        >
          로그아웃
        </button>
      </div>

      <div className="tabs">
        <button
          className={`tab ${tab === "inquiries" ? "active" : ""}`}
          onClick={() => setTab("inquiries")}
        >
          문의
        </button>
        <button
          className={`tab ${tab === "bugs" ? "active" : ""}`}
          onClick={() => setTab("bugs")}
        >
          버그 신고
        </button>
        <button
          className={`tab ${tab === "users" ? "active" : ""}`}
          onClick={() => setTab("users")}
        >
          회원 탈퇴
        </button>
      </div>

      {tab === "inquiries" ? (
        <InquiriesPanel />
      ) : tab === "bugs" ? (
        <BugReportsPanel />
      ) : (
        <UsersPanel />
      )}
    </div>
  );
}

function InquiriesPanel() {
  const [filter, setFilter] = useState<InquiryStatus | "ALL">("ALL");
  const [items, setItems] = useState<Inquiry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setItems(await listInquiries(filter === "ALL" ? undefined : filter));
    } catch (err) {
      setError(err instanceof Error ? err.message : "불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }, [filter]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <>
      <div className="filters">
        {(["ALL", "PENDING", "ANSWERED"] as const).map((f) => (
          <button
            key={f}
            className={`chip ${filter === f ? "active" : ""}`}
            onClick={() => setFilter(f)}
          >
            {f === "ALL" ? "전체" : f === "PENDING" ? "대기" : "답변완료"}
          </button>
        ))}
      </div>

      {error ? <p className="error">{error}</p> : null}
      {loading ? (
        <p className="empty">불러오는 중…</p>
      ) : items.length === 0 ? (
        <p className="empty">문의가 없습니다.</p>
      ) : (
        items.map((item) => (
          <InquiryCard key={item.inquiryId} inquiry={item} onAnswered={load} />
        ))
      )}
    </>
  );
}

function InquiryCard({
  inquiry,
  onAnswered,
}: {
  inquiry: Inquiry;
  onAnswered: () => void;
}) {
  const [answer, setAnswer] = useState(inquiry.answer ?? "");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async () => {
    if (!answer.trim()) return;
    setSubmitting(true);
    setError(null);
    try {
      await answerInquiry(inquiry.inquiryId, answer.trim());
      onAnswered();
    } catch (err) {
      setError(err instanceof Error ? err.message : "저장에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="card">
      <div className="card-head">
        <span className={`badge ${inquiry.status.toLowerCase()}`}>
          {inquiry.status === "PENDING" ? "대기" : "답변완료"}
        </span>
        <span className="badge">{CATEGORY_LABEL[inquiry.category]}</span>
        <span className="card-title">{inquiry.title}</span>
      </div>
      <p className="meta">
        {inquiry.nickname} (#{inquiry.userId}) · {formatDate(inquiry.createdAt)}
      </p>
      <p className="body-text">{inquiry.body}</p>

      <div className="answer-box">
        <label htmlFor={`answer-${inquiry.inquiryId}`}>답변</label>
        <textarea
          id={`answer-${inquiry.inquiryId}`}
          maxLength={1000}
          placeholder="답변을 입력하세요"
          value={answer}
          onChange={(e) => setAnswer(e.target.value)}
        />
        {error ? <p className="error">{error}</p> : null}
        <button
          className="btn small"
          disabled={submitting || !answer.trim()}
          onClick={submit}
        >
          {submitting
            ? "저장 중…"
            : inquiry.status === "ANSWERED"
              ? "답변 수정"
              : "답변 등록"}
        </button>
      </div>
    </div>
  );
}

function BugReportsPanel() {
  const [items, setItems] = useState<BugReport[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setItems(await listBugReports());
    } catch (err) {
      setError(err instanceof Error ? err.message : "불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const toggle = async (report: BugReport) => {
    setBusyId(report.bugReportId);
    try {
      await updateBugReportStatus(
        report.bugReportId,
        report.status === "RECEIVED" ? "RESOLVED" : "RECEIVED",
      );
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "상태 변경에 실패했습니다.");
    } finally {
      setBusyId(null);
    }
  };

  if (error) {
    return <p className="error">{error}</p>;
  }
  if (loading) {
    return <p className="empty">불러오는 중…</p>;
  }
  if (items.length === 0) {
    return <p className="empty">버그 신고가 없습니다.</p>;
  }

  return (
    <>
      {items.map((report) => (
        <div className="card" key={report.bugReportId}>
          <div className="card-head">
            <span className={`badge ${report.status.toLowerCase()}`}>
              {report.status === "RECEIVED" ? "접수" : "처리완료"}
            </span>
            <span className="meta">
              {report.nickname} (#{report.userId}) ·{" "}
              {formatDate(report.createdAt)}
            </span>
          </div>
          <p className="body-text">{report.body}</p>
          <div className="row-actions">
            <button
              className="btn ghost small"
              disabled={busyId === report.bugReportId}
              onClick={() => toggle(report)}
            >
              {report.status === "RECEIVED" ? "처리완료로 표시" : "접수로 되돌리기"}
            </button>
          </div>
        </div>
      ))}
    </>
  );
}

// 앱을 쓸 수 없는 회원이 메일로 보낸 계정 삭제 요청을 처리한다(계정 삭제 안내 페이지의 메일 요청 경로).
// 앱에서 탈퇴할 때와 같은 범위로 지워진다.
function UsersPanel() {
  const [keyword, setKeyword] = useState("");
  const [items, setItems] = useState<AdminUser[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);

  const search = async (event: FormEvent) => {
    event.preventDefault();
    if (!keyword.trim()) return;
    setLoading(true);
    setError(null);
    setNotice(null);
    try {
      setItems(await searchUsers(keyword.trim()));
    } catch (err) {
      setError(err instanceof Error ? err.message : "찾지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const withdraw = async (user: AdminUser) => {
    const confirmed = window.confirm(
      `${user.nickname} (#${user.userId}) 회원을 탈퇴 처리할까요? 되돌릴 수 없습니다.`,
    );
    if (!confirmed) return;
    setBusyId(user.userId);
    setError(null);
    try {
      await withdrawUser(user.userId);
      setItems((prev) => prev?.filter((u) => u.userId !== user.userId) ?? null);
      setNotice(
        `${user.nickname} (#${user.userId}) 회원을 탈퇴 처리했습니다. 요청한 분께 결과를 메일로 알려 주세요.`,
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : "탈퇴 처리에 실패했습니다.");
    } finally {
      setBusyId(null);
    }
  };

  return (
    <>
      <form className="search-row" onSubmit={search}>
        <input
          placeholder="가입 이메일 또는 닉네임"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
        <button className="btn small" disabled={loading || !keyword.trim()}>
          찾기
        </button>
      </form>
      <p className="meta search-help">
        메일로 온 계정 삭제 요청을 처리합니다. 보낸 주소가 가입 이메일과 같은지
        먼저 확인하세요.
      </p>

      {notice ? <p className="notice">{notice}</p> : null}
      {error ? <p className="error">{error}</p> : null}
      {loading ? (
        <p className="empty">찾는 중…</p>
      ) : items === null ? null : items.length === 0 ? (
        <p className="empty">일치하는 회원이 없습니다.</p>
      ) : (
        items.map((user) => (
          <div className="card" key={user.userId}>
            <div className="card-head">
              <span className="badge">{USER_STATUS_LABEL[user.status]}</span>
              <span className="card-title">{user.nickname}</span>
              <span className="meta">#{user.userId}</span>
            </div>
            <p className="meta">
              {user.email ?? "이메일 없음"} · 가입 방법{" "}
              {user.socialProviders.length === 0
                ? "이메일"
                : user.socialProviders.map((p) => PROVIDER_LABEL[p]).join(", ")}{" "}
              · 가입 {formatDate(user.createdAt)}
              {user.lastLoginAt
                ? ` · 마지막 로그인 ${formatDate(user.lastLoginAt)}`
                : ""}
            </p>
            <div className="row-actions">
              <button
                className="btn small danger"
                disabled={busyId === user.userId || user.status === "WITHDRAWN"}
                onClick={() => withdraw(user)}
              >
                {busyId === user.userId ? "처리 중…" : "탈퇴 처리"}
              </button>
            </div>
          </div>
        ))
      )}
    </>
  );
}
