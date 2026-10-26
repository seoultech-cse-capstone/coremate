"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";

import {
  answerInquiry,
  clearToken,
  getToken,
  listBugReports,
  listInquiries,
  updateBugReportStatus,
} from "@/lib/api";
import {
  CATEGORY_LABEL,
  type BugReport,
  type Inquiry,
  type InquiryStatus,
} from "@/lib/types";

type Tab = "inquiries" | "bugs";

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
      </div>

      {tab === "inquiries" ? <InquiriesPanel /> : <BugReportsPanel />}
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
