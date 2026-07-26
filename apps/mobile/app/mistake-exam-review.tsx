import { router, useLocalSearchParams } from "expo-router";
import { useEffect, useMemo } from "react";

import { useAuth } from "../src/features/auth";
import { MistakeExamReview } from "../src/features/logWeak/components/MistakeExamReview";
import type { SolveReviewItem } from "../src/features/solve/types";

export default function MistakeExamReviewScreen() {
  const { status } = useAuth();
  const { category, examType, reviewItemsJson, sessionType, subject } =
    useLocalSearchParams<{
      category?: string;
      examType?: string;
      reviewItemsJson?: string;
      sessionType?:
        | "SUBJECT_MOCK"
        | "CHAPTER_MOCK"
        | "CONCEPT_PRACTICE"
        | "DAILY_QUIZ";
      subject?: string;
    }>();

  const results = useMemo<SolveReviewItem[] | undefined>(() => {
    if (!reviewItemsJson) {
      return undefined;
    }
    try {
      return JSON.parse(reviewItemsJson) as SolveReviewItem[];
    } catch (error) {
      console.error("[mistake-exam-review] reviewItemsJson 파싱 실패", error);
      return undefined;
    }
  }, [reviewItemsJson]);

  // 게스트는 개념 문제집 해설만 볼 수 있다. 그 외(모의고사 등) 기록 화면은 회원 전용.
  const isBlockedForGuest =
    status === "guest" && sessionType !== "CONCEPT_PRACTICE";

  useEffect(() => {
    if (isBlockedForGuest) {
      router.replace("/login-required");
    }
  }, [isBlockedForGuest]);

  if (isBlockedForGuest) {
    return null;
  }

  return (
    <MistakeExamReview
      category={category ?? "Arrays"}
      examType={examType ?? "모의고사"}
      sessionType={sessionType}
      subject={subject ?? "Data Structure"}
      results={results}
      onBack={() => router.back()}
    />
  );
}
